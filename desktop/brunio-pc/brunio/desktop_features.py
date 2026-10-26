"""PC-only screens: downloads, shared calendar, scores and split screen."""
from __future__ import annotations
import datetime as dt, hashlib, os, re, threading, time
from pathlib import Path
from urllib.parse import quote, urlsplit
from urllib.request import Request, urlopen
from PySide6.QtCore import Qt,QTimer,QDate
from PySide6.QtGui import QPixmap
from PySide6.QtWidgets import (QWidget,QLabel,QPushButton,QVBoxLayout,QHBoxLayout,QGridLayout,QComboBox,QCalendarWidget,QDialog,QScrollArea,QMessageBox,QTableWidget,QTableWidgetItem)
from .downloads import DownloadStore,file_extension
from .core import valid_url

def _button(text,fn):
    w=QPushButton(text);w.setCursor(Qt.CursorShape.PointingHandCursor);w.clicked.connect(lambda checked=False:fn());return w

def _label(text,size=14):
    w=QLabel(str(text));w.setWordWrap(True);w.setTextFormat(Qt.TextFormat.PlainText);w.setStyleSheet(f'font-size:{size}px;');return w

def owner(client):return str(client.session.get('userId','local'))+':'+str(client.profile)

def local_day(value):
    try:return dt.datetime.fromisoformat(str(value).replace('Z','+00:00')).astimezone().strftime('%Y-%m-%d')
    except (ValueError,TypeError):return str(value)[:10]

def scoreboard(transport,league,month=None):
    if league=='F1' and month:
        year=month[:4];data=transport('https://api.jolpi.ca/ergast/f1/'+year+'.json?limit=100')
        races=data.get('MRData',{}).get('RaceTable',{}).get('Races',[])
        return [{'id':str(x.get('round','')),'league':'F1','title':x.get('raceName','F1'),'starts':x.get('date','')+'T'+x.get('time','00:00:00Z'),'teams':[],'scores':[],'logos':[],'status':''} for x in races if x.get('date','').startswith(month)]
    if league in ('NBA','F1'):
        sport='basketball/nba' if league=='NBA' else 'racing/f1'
        suffix=('?dates='+month.replace('-','')+'&limit=1000') if month and league=='NBA' else ''
        data=transport('https://site.api.espn.com/apis/site/v2/sports/'+sport+'/scoreboard'+suffix)
        result=[]
        for event in data.get('events',[]):
            comp=(event.get('competitions') or [{}])[0];teams=comp.get('competitors') or []
            result.append({'id':str(event.get('id','')),'league':league,'title':event.get('name',''),'starts':event.get('date',''),
                'teams':[str((x.get('team') or {}).get('id','')) for x in teams],
                'scores':[str(x.get('score','')) for x in teams],
                'logos':[(x.get('team') or {}).get('logo','') for x in teams],
                'status':(event.get('status') or comp.get('status') or {}).get('type',{}).get('shortDetail',''),
                'drivers':[{'name':(x.get('athlete') or {}).get('shortName') or (x.get('athlete') or {}).get('displayName',''),
                    'position':x.get('order','—'),'photo':((x.get('athlete') or {}).get('headshot') or {}).get('href',''),
                    'stats':{v.get('name',''):v.get('displayValue','—') for v in x.get('statistics',[])}} for x in teams if x.get('athlete')]})
        return result
    if league in ('MLB','LIDOM'):
        if month:
            first=dt.date.fromisoformat(month+'-01');last=(first.replace(day=28)+dt.timedelta(days=4)).replace(day=1)-dt.timedelta(days=1)
        else:first=last=dt.date.today()
        sport='1' if league=='MLB' else '17&leagueId=131'
        data=transport(f'https://statsapi.mlb.com/api/v1/schedule?sportId={sport}&startDate={first}&endDate={last}')
        result=[]
        for day in data.get('dates',[]):
            for g in day.get('games',[]):
                teams=g.get('teams',{});away=teams.get('away',{});home=teams.get('home',{});a=away.get('team',{});h=home.get('team',{})
                result.append({'id':str(g.get('gamePk','')),'league':league,'title':a.get('name','')+' vs '+h.get('name',''),
                    'starts':g.get('gameDate',day.get('date','')),'teams':[str(a.get('id','')),str(h.get('id',''))],
                    'scores':[str(away.get('score','')),str(home.get('score',''))],
                    'logos':[f"https://www.mlbstatic.com/team-logos/{x.get('id','')}.svg" for x in (a,h)] if league=='MLB' else [],
                    'status':(g.get('status') or {}).get('detailedState','')})
        return result
    return []

def game_details(transport,event):
    league=event['league'];game=event.get('id','')
    if league=='F1':
        drivers=sorted(event.get('drivers',[]),key=lambda x:int(x['position']) if str(x['position']).isdigit() else 999)
        rows=[[str(x['position']),x['name'],x['stats'].get('lapsCompleted',x['stats'].get('laps','—')),x['stats'].get('gapToLeader',x['stats'].get('gap','—'))] for x in drivers]
        return [('F1',['Pos','Piloto','Vueltas','Diferencia'],rows)]
    if not re.fullmatch(r'\d{1,16}',str(game)):return []
    if league=='NBA':
        body=transport('https://site.api.espn.com/apis/site/v2/sports/basketball/nba/summary?event='+str(game));groups=[]
        for team in body.get('boxscore',{}).get('players',[]):
            for table in team.get('statistics',[]):
                columns=table.get('labels') or table.get('names') or []
                rows=[[a.get('athlete',{}).get('displayName','')] + [str(v) for v in a.get('stats',[])] for a in table.get('athletes',[]) if not a.get('didNotPlay')]
                groups.append((team.get('team',{}).get('displayName','NBA'),['Jugador']+columns,rows))
        return groups
    body=transport('https://statsapi.mlb.com/api/v1.1/game/'+str(game)+'/feed/live');groups=[]
    for side in ('away','home'):
        team=body.get('liveData',{}).get('boxscore',{}).get('teams',{}).get(side,{})
        rows=[]
        for player in team.get('players',{}).values():
            batting=(player.get('stats') or {}).get('batting') or {}
            if batting:
                rows.append([player.get('person',{}).get('fullName','')]+[str(batting.get(k,'—')) for k in ('atBats','runs','hits','rbi','baseOnBalls','strikeOuts')])
        groups.append((team.get('team',{}).get('name',side),['Jugador','AB','R','H','RBI','BB','K'],rows))
    return groups

def follows(subs,event):
    league=event['league'];return league+':*' in subs or 'EVENT:'+league+':'+event['id'] in subs or any(league+':'+x in subs for x in event.get('teams',[]))

class DesktopFeatures:
    def setup_desktop_features(self):
        self.download_store=None;self.download_stop=threading.Event();self.multi_players=[];self.multi_page=None;self.live_candidates=[]
    def store(self):
        if self.download_store is None:self.download_store=DownloadStore()
        return self.download_store
    def _cache_cover(self,item):
        url=item.get('poster','');u=urlsplit(url)
        if u.scheme!='https' or u.hostname!='image.tmdb.org':return
        dest=self.store().root/('cover-'+hashlib.sha256(url.encode()).hexdigest()+'.img')
        if dest.exists():return
        try:
            with urlopen(Request(url,headers={'User-Agent':'BruniO-PC'}),timeout=10) as response:
                data=response.read(2*1024*1024+1)
            if 0<len(data)<=2*1024*1024:dest.write_bytes(data)
        except Exception:pass
    def _start_download(self,item,season,episode,stream):
        ext=file_extension(stream)
        if not ext:self.tell(self.t('Esta fuente no ofrece un archivo descargable.','This source does not provide a downloadable file.'));return
        scope=owner(self.client);store=self.store();entry,created=store.create(scope,item,season,episode,ext)
        if not created:self.tell(self.t('Ya está descargado o en cola.','Already downloaded or queued.'));return
        self.tell(self.t('Descarga iniciada.','Download started.'))
        def work():
            self._cache_cover(item)
            return store.transfer(entry,stream,cancel=lambda:self.download_stop.is_set() or scope!=owner(self.client))
        self.run(work,lambda _:self.tell(self.t('Descarga completada.','Download completed.')),guard=False)
    def download_title(self,item,season=0,episode=0,replace=None):
        scope=owner(self.client);self.tell(self.t('Buscando archivo descargable…','Finding a downloadable file…'))
        def done(value):
            if scope!=owner(self.client):return
            streams,_=value;stream=next((x for x in streams if file_extension(x)),None)
            if stream:
                if replace is not None:self.store().remove(replace['id'],scope)
                self._start_download(item,season,episode,stream)
            else:self.tell(self.t('No hay archivos directos disponibles.','No direct file is available.'))
        self.run(lambda:self.client.streams(item,season,episode,self.preferences.get('limit',0)),done)
    def download_season(self,item,season):
        scope=owner(self.client)
        def prepare():
            today=dt.date.today().isoformat();episodes=[e for e in self.client.episodes(item,season) if e.get('air_date') and e['air_date']<=today]
            if not episodes:raise ValueError('No released episodes available')
            result=[]
            for ep in episodes:
                if scope!=owner(self.client) or self.download_stop.is_set():break
                number=ep['episode_number']
                try:
                    streams,_=self.client.streams(item,season,number,self.preferences.get('limit',0));stream=next((x for x in streams if file_extension(x)),None)
                except Exception:stream=None
                result.append((number,stream))
            return result
        def ready(rows):
            if scope!=owner(self.client):return
            count=0
            for episode,stream in rows:
                if stream:
                    self._start_download(item,season,episode,stream);count+=1
            self.tell(self.t(f'Temporada: {count}/{len(rows)} en cola.',f'Season: {count}/{len(rows)} queued.'))
        self.tell(self.t('Preparando temporada…','Preparing season…'));self.run(prepare,ready)
    def downloads(self):
        self.select_nav('downloads');layout,page=self.page(self.t('Descargas','Downloads'));scope=owner(self.client)
        hint=_label(self.t('Tus películas y series, listas para disfrutar sin conexión.','Your movies and shows, ready to enjoy offline.'),14);hint.setObjectName('muted');layout.addWidget(hint)
        holder=QWidget();box=QVBoxLayout(holder);box.setContentsMargins(0,0,0,0);layout.addWidget(holder)
        last={'rows':None}
        def refresh():
            if page!=self.page_id or scope!=owner(self.client):timer.stop();return
            entries=self.store().list(scope)
            if entries==last['rows']:return
            last['rows']=entries
            while box.count():
                x=box.takeAt(0)
                if x.widget():x.widget().deleteLater()
            if not entries:
                empty=QWidget();empty.setObjectName('downloadCard');empty.setMaximumWidth(720);empty_l=QVBoxLayout(empty);empty_l.setContentsMargins(28,26,28,26)
                empty_l.addWidget(_label('↓',32));empty_l.addWidget(_label(self.t('Tu colección offline','Your offline collection'),20));empty_l.addWidget(_label(self.t('Descarga una película o episodios desde sus detalles y aparecerán aquí.','Download a movie or episodes from their details and they will appear here.'),14));box.addWidget(empty)
            grouped={}
            for entry in entries:grouped.setdefault((entry['cloud'],entry['season']),[]).append(entry)
            for (_,season),rows in grouped.items():
                first=rows[0];panel=QWidget();panel.setObjectName('downloadCard');pl=QHBoxLayout(panel);pl.setContentsMargins(16,16,16,16);pl.setSpacing(18)
                cover=QLabel();cover.setFixedSize(80,116);cover.setScaledContents(True);cover.setObjectName('cover');pl.addWidget(cover)
                local=self.store().root/('cover-'+hashlib.sha256(first['poster'].encode()).hexdigest()+'.img')
                if local.exists():cover.setPixmap(QPixmap(str(local)))
                elif first['poster']:self.image(first['poster'],cover,80,116)
                content=QVBoxLayout();content.addWidget(_label(first['title'],20))
                if season:content.addWidget(_label(self.t('Temporada ','Season ')+str(season),13))
                for entry in sorted(rows,key=lambda x:x['episode']):
                    line=QHBoxLayout();size=entry['total'];percent=f"{entry['done']*100//size}%" if size else f"{entry['done']//1048576} MB"
                    text={'complete':self.t('Lista','Ready'),'failed':self.t('Falló','Failed'),'interrupted':self.t('Interrumpida','Interrupted'),'queued':self.t('En cola','Queued'),'downloading':self.t('Descargando','Downloading')}.get(entry['state'],entry['state'])
                    line.addWidget(_label((f"E{entry['episode']} · " if season else '')+text+(f' · {percent}' if entry['state']=='downloading' else '')+((' · '+entry['error']) if entry['error'] else '')),1)
                    if entry['state']=='complete' and self.store().path(entry).is_file():line.addWidget(_button('▶',lambda e=entry:self.play_download(e)))
                    if entry['state'] in ('failed','interrupted') or (entry['state']=='complete' and not self.store().path(entry).is_file()):line.addWidget(_button(self.t('Reintentar','Retry'),lambda e=entry:self.retry_download(e)))
                    if entry['state'] not in ('queued','downloading'):line.addWidget(_button('✕',lambda e=entry:self.delete_download(e,refresh)))
                    content.addLayout(line)
                pl.addLayout(content,1);box.addWidget(panel)
            box.addStretch()
        timer=QTimer(holder);timer.timeout.connect(refresh);timer.start(1800);refresh()
    def play_download(self,entry):
        if entry['owner']!=owner(self.client):return
        path=self.store().path(entry)
        if not path.is_file():self.tell(self.t('Archivo no disponible.','File unavailable.'));return
        item={'id':entry['media_id'],'cloudId':entry['cloud'],'title':entry['title'],'type':entry['kind'],'year':entry['year'],'poster':entry['poster'],'backdrop':'','overview':'','rating':0}
        self.play_id+=1;self.play_stream(item,entry['season'],entry['episode'],0,{'url':path.as_uri(),'name':'Offline'})
    def retry_download(self,entry):
        if entry['owner']!=owner(self.client):return
        item={'id':entry['media_id'],'cloudId':entry['cloud'],'title':entry['title'],'type':entry['kind'],'year':entry['year'],'poster':entry['poster'],'backdrop':'','overview':'','rating':0}
        self.download_title(item,entry['season'],entry['episode'],replace=entry)
    def delete_download(self,entry,refresh):
        if QMessageBox.question(self,'BruniO',self.t('¿Eliminar este archivo descargado?','Delete this downloaded file?'))==QMessageBox.StandardButton.Yes:
            self.store().remove(entry['id'],owner(self.client));refresh()
    def calendar(self):
        self.select_nav('calendar');layout,page=self.page(self.t('Calendario','Calendar'));layout.addWidget(_label(self.t('Eventos y deportes seguidos en tu cuenta de BruniO.','Events and sports followed in your BruniO account.')))
        tools=QHBoxLayout();tools.setSpacing(20);calendar=QCalendarWidget();calendar.setGridVisible(False);calendar.setVerticalHeaderFormat(QCalendarWidget.VerticalHeaderFormat.NoVerticalHeader);calendar.setFixedWidth(440);calendar.setMinimumHeight(330);tools.addWidget(calendar,0,Qt.AlignmentFlag.AlignTop)
        side=QWidget();side.setObjectName('downloadCard');side.setMaximumWidth(650);right=QVBoxLayout(side);right.setContentsMargins(22,22,22,22);right.addWidget(_label(self.t('Tu agenda','Your agenda'),21))
        refresh_button=_button(self.t('Actualizar','Refresh'),lambda:load());refresh_button.setMaximumWidth(150);right.addWidget(refresh_button)
        items=QWidget();items_l=QVBoxLayout(items);items_l.setContentsMargins(0,8,0,0);items_l.setAlignment(Qt.AlignmentFlag.AlignTop);right.addWidget(items);right.addStretch();tools.addWidget(side,1);tools.addStretch();layout.addLayout(tools)
        data={'shared':[],'sports':[],'subs':{},'warning':''}
        def render():
            while items_l.count():
                x=items_l.takeAt(0)
                if x.widget():x.widget().deleteLater()
            day=calendar.selectedDate().toString('yyyy-MM-dd');matches=[x for x in data['shared'] if local_day(x.get('starts_at',''))==day]
            sports=[x for x in data['sports'] if local_day(x.get('starts',''))==day]
            if data['warning']:items_l.addWidget(_label(data['warning'],13))
            for event in matches:
                card=QWidget();card.setObjectName('downloadCard');cl=QHBoxLayout(card);cl.addWidget(_label(event.get('title','Event')+' · '+event.get('starts_at','')[:16],16),1)
                if event.get('cloud_id'):cl.addWidget(_button(self.t('Ver','Open'),lambda e=event:self.run(lambda:self.client.by_id(e['cloud_id']),self.open)))
                items_l.addWidget(card)
            for event in sports:items_l.addWidget(_label(event['league']+' · '+event['title']+' · '+event.get('status',''),16))
            if not matches and not sports:items_l.addWidget(_label(self.t('Sin eventos para este día.','No events on this day.')))
            items_l.addStretch()
        def load():
            month=calendar.selectedDate().toString('yyyy-MM');self.tell(self.t('Actualizando calendario…','Refreshing calendar…'))
            def fetch():
                shared=self.client.shared_calendar();warning=''
                try:subs=self.client.calendar_subscriptions()
                except Exception:subs={};warning=self.t('Suscripciones deportivas no disponibles. Verifica SQL RP134.','Sports subscriptions unavailable. Check RP134 SQL.')
                active={k for k,v in subs.items() if isinstance(v,dict) and v.get('enabled')}
                sports=[]
                for league in ('NBA','MLB','LIDOM','F1'):
                    if any(k.startswith(league+':') or k.startswith('EVENT:'+league+':') for k in active):
                        try:sports.extend(x for x in scoreboard(self.client.transport,league,month) if follows(active,x))
                        except Exception:warning=self.t('Algunos calendarios deportivos no están disponibles.','Some sports schedules are unavailable.')
                return shared,sports,subs,warning
            def done(value):
                if page!=self.page_id:return
                data.update(zip(('shared','sports','subs','warning'),value));self.tell('');render()
            self.run(fetch,done)
        calendar.selectionChanged.connect(render);calendar.currentPageChanged.connect(lambda *_:load());load()
    def sports_scores(self):
        dialog=QDialog(self);dialog.setWindowTitle('BruniO · Live Stats');dialog.resize(720,580);layout=QVBoxLayout(dialog);league=QComboBox();league.addItems(['NBA','MLB','LIDOM','F1']);layout.addWidget(league)
        scroll=QScrollArea();scroll.setWidgetResizable(True);body=QWidget();box=QVBoxLayout(body);scroll.setWidget(body);layout.addWidget(scroll)
        def load():
            current=league.currentText()
            def done(rows):
                if not dialog.isVisible() or current!=league.currentText():return
                while box.count():
                    x=box.takeAt(0)
                    if x.widget():x.widget().deleteLater()
                if not rows:box.addWidget(_label(self.t('Sin eventos disponibles.','No events available.')))
                for event in rows:
                    panel=QWidget();panel.setObjectName('downloadCard');line=QHBoxLayout(panel)
                    for logo in event['logos'][:2]:
                        if logo:
                            image=QLabel();image.setFixedSize(38,38);image.setScaledContents(True);self.image(logo,image,38,38);line.addWidget(image)
                    line.addWidget(_label(event['title'],16),1);line.addWidget(_label(' : '.join(x for x in event['scores'] if x) or event['status'],14))
                    line.addWidget(_button('Stats',lambda e=event:self.sports_game_details(e)));box.addWidget(panel)
                box.addStretch()
            self.run(lambda:scoreboard(self.client.transport,current),done)
        league.currentTextChanged.connect(load);layout.addWidget(_button(self.t('Actualizar','Refresh'),load));QTimer.singleShot(0,load);dialog.exec()
    def sports_game_details(self,event):
        d=QDialog(self);d.setWindowTitle(event['title']+' · Stats');d.resize(960,650);layout=QVBoxLayout(d)
        layout.addWidget(_label(event['title'],22));scroll=QScrollArea();scroll.setWidgetResizable(True);body=QWidget();box=QVBoxLayout(body);scroll.setWidget(body);layout.addWidget(scroll)
        def render(groups):
            if not d.isVisible():return
            while box.count():
                x=box.takeAt(0)
                if x.widget():x.widget().deleteLater()
            if not groups or all(not rows for _,_,rows in groups):box.addWidget(_label(self.t('Estadísticas no disponibles para este evento.','Stats are unavailable for this event.')))
            for name,columns,rows in groups:
                if not rows:continue
                box.addWidget(_label(name,18));table=QTableWidget(len(rows),len(columns));table.setHorizontalHeaderLabels(columns)
                table.setEditTriggers(QTableWidget.EditTrigger.NoEditTriggers);table.setMinimumHeight(min(480,40+len(rows)*32))
                for i,row in enumerate(rows):
                    for j,value in enumerate(row[:len(columns)]):table.setItem(i,j,QTableWidgetItem(str(value)))
                    if event['league']=='F1' and i<len(event.get('drivers',[])):
                        driver=sorted(event['drivers'],key=lambda x:int(x['position']) if str(x['position']).isdigit() else 999)[i]
                        if driver.get('photo'):
                            cell=QWidget();cell_layout=QHBoxLayout(cell);cell_layout.setContentsMargins(3,0,3,0);image=QLabel();image.setFixedSize(34,34);image.setScaledContents(True);self.image(driver['photo'],image,34,34);cell_layout.addWidget(image);cell_layout.addWidget(_label(driver['name'],12));table.setCellWidget(i,1,cell);table.setRowHeight(i,38)
                table.horizontalHeader().setStretchLastSection(True);table.resizeColumnsToContents();box.addWidget(table)
            box.addStretch()
        def load():self.run(lambda:game_details(self.client.transport,event),render)
        layout.addWidget(_button(self.t('Actualizar','Refresh'),load));QTimer.singleShot(0,load);d.exec()
    def open_multiview(self):
        if not self.current or self.current['item']['type']!='live':self.tell(self.t('Abre un canal en vivo primero.','Open a live channel first.'));return
        if self.client.party:self.tell(self.t('Sal de Watch Party antes de usar Multiview.','Leave Watch Party before using Multiview.'));return
        current=self.current;self.player.stop();self.current=None;self.multi_players=[]
        page=QWidget();page.setObjectName('multiPage');outer=QVBoxLayout(page);top=QHBoxLayout();outer.addLayout(top)
        top.addWidget(_button('← '+self.t('Salir','Exit'),lambda:(self.close_multiview(),self.live())))
        top.addWidget(_button('+ '+self.t('Agregar señal','Add stream'),self.multi_choose));top.addWidget(_button('Stats',self.sports_scores));top.addStretch()
        self.multi_grid=QGridLayout();self.multi_grid.setSpacing(8);outer.addLayout(self.multi_grid,1);self.multi_page=page;self.stack.addWidget(page);self.stack.setCurrentWidget(page)
        self.multi_add(current['item'],current['stream'])
    def multi_add(self,item,stream):
        if len(self.multi_players)>=4:self.tell(self.t('Máximo 4 señales.','Maximum 4 streams.'));return
        if not valid_url(stream.get('url','')):self.tell('Invalid stream');return
        container=QWidget();container.setObjectName('multiTile');v=QVBoxLayout(container);v.setContentsMargins(4,4,4,4)
        video=QWidget();video.setAttribute(Qt.WidgetAttribute.WA_NativeWindow);video.setStyleSheet('background:#000;');v.addWidget(video,1)
        bar=QHBoxLayout();bar.addWidget(_label(item['title'],13),1);v.addLayout(bar)
        player=self.library.media_player_new();media=self.library.media_new(stream['url'])
        hints=(stream.get('behaviorHints') or {}).get('proxyHeaders',{}).get('request',{})
        for header,opt in [('User-Agent','http-user-agent'),('Referer','http-referrer')]:
            if hints.get(header):media.add_option(':'+opt+'='+hints[header])
        player.set_media(media);media.release();record={'player':player,'widget':container,'video':video,'item':item}
        self.multi_players.append(record);self.multi_grid.addWidget(container,(len(self.multi_players)-1)//2,(len(self.multi_players)-1)%2)
        if os.name=='nt':player.set_hwnd(int(video.winId()))
        else:player.set_xwindow(int(video.winId()))
        bar.addWidget(_button('🔊',lambda r=record:self.multi_audio(r)));bar.addWidget(_button('✕',lambda r=record:self.multi_remove(r)))
        player.play();self.multi_audio(self.multi_players[0])
    def multi_audio(self,selected):
        for entry in self.multi_players:entry['player'].audio_set_mute(entry is not selected)
    def multi_remove(self,record):
        if record not in self.multi_players:return
        self.multi_players.remove(record);record['player'].stop();record['player'].release();self.multi_grid.removeWidget(record['widget']);record['widget'].deleteLater()
        for i,entry in enumerate(self.multi_players):self.multi_grid.addWidget(entry['widget'],i//2,i%2)
        if self.multi_players:self.multi_audio(self.multi_players[0])
        else:self.close_multiview();self.live()
    def multi_choose(self):
        if not self.live_candidates:self.tell(self.t('Abre Live TV y selecciona una categoría primero.','Open Live TV and select a category first.'));return
        dialog=QDialog(self);dialog.setWindowTitle(self.t('Agregar señal','Add stream'));dialog.resize(700,600);layout=QVBoxLayout(dialog);scroll=QScrollArea();scroll.setWidgetResizable(True);body=QWidget();items=QVBoxLayout(body);scroll.setWidget(body);layout.addWidget(scroll)
        for channel,provider,manifest in self.live_candidates[:100]:
            def select(channel=channel,provider=provider,manifest=manifest):
                dialog.accept();item={'title':channel.get('name','Channel'),'type':'live'}
                if provider in ('tv','m3u'):self.multi_add(item,{'url':channel['url'],'name':channel.get('name','Channel'),'behaviorHints':{'proxyHeaders':{'request':channel.get('headers',{})}}});return
                url=manifest.removesuffix('/manifest.json')+f"/stream/{quote(channel.get('type','tv'))}/{quote(channel['id'],safe='')}.json"
                def done(j):
                    rows=[x for x in j.get('streams',[]) if valid_url(x.get('url',''))]
                    if rows and self.multi_page is not None:self.multi_add(item,rows[0])
                    elif self.multi_page is not None:self.tell(self.t('Sin señales disponibles.','No streams available.'))
                self.run(lambda:self.client.transport(url),done)
            items.addWidget(_button(channel.get('name','Channel'),select))
        dialog.exec()
    def close_multiview(self):
        for entry in self.multi_players:entry['player'].stop();entry['player'].release()
        self.multi_players=[]
        if self.multi_page is not None:
            self.stack.removeWidget(self.multi_page);self.multi_page.deleteLater();self.multi_page=None
