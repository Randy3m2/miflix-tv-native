from __future__ import annotations
import json, os, random, sys, time, uuid, webbrowser
from pathlib import Path
from urllib.parse import urlencode,quote
from PySide6.QtCore import Qt,QObject,Signal,QRunnable,QThreadPool,QTimer,QUrl,QSize
from PySide6.QtGui import QPixmap,QIcon,QImage,QPainter
from PySide6.QtNetwork import QNetworkAccessManager,QNetworkRequest
from PySide6.QtWidgets import (QApplication,QMainWindow,QWidget,QVBoxLayout,QHBoxLayout,QPushButton,QLabel,QLineEdit,QScrollArea,QStackedWidget,QDialog,QComboBox,QSlider,QInputDialog,QMessageBox,QFileDialog,QCheckBox,QDoubleSpinBox,QGridLayout)
from shiboken6 import isValid
from .core import Client,StaleWork,SUPABASE_URL,SUPABASE_KEY,LIVE,REPO,iso,now_ms,media,stream_size,stream_quality,normalize,valid_url
from .vault import Vault
from .visuals import Art,Poster,icon
from .desktop_features import DesktopFeatures

def java_hash(value):
    h=0
    encoded=value.encode('utf-16-be')
    for i in range(0,len(encoded),2):h=(31*h+int.from_bytes(encoded[i:i+2],'big'))&0xffffffff
    return h if h<2**31 else h-2**32
from . import VERSION

ASSETS=Path(getattr(sys,'_MEIPASS',Path(__file__).resolve().parent.parent))/'assets'
class Signals(QObject):
    result=Signal(object);error=Signal(str);finished=Signal()
class Task(QRunnable):
    def __init__(self,fn):super().__init__();self.fn=fn;self.signals=Signals()
    def run(self):
        try:self.signals.result.emit(self.fn())
        except StaleWork:pass
        except Exception as e:
            import re
            self.signals.error.emit(re.sub(r'https?://[^\s]+','[enlace]',str(e),flags=re.I)[:240])
        finally:self.signals.finished.emit()

def row():
    w=QWidget();layout=QHBoxLayout(w);layout.setContentsMargins(0,0,0,0);layout.setSpacing(12);return w,layout
def column():
    w=QWidget();layout=QVBoxLayout(w);layout.setContentsMargins(24,20,24,28);layout.setSpacing(16);return w,layout
def label(text,size=16):
    w=QLabel(text);w.setWordWrap(True);w.setStyleSheet(f'font-size:{size}px;');w.setTextFormat(Qt.TextFormat.PlainText);return w

def button(text,fn):
    b=QPushButton(text);b.setCursor(Qt.CursorShape.PointingHandCursor);b.clicked.connect(lambda checked=False:fn());return b
class Window(DesktopFeatures,QMainWindow):
    player_event=Signal(str)
    def __init__(self,smoke=False):
        super().__init__();self.setWindowTitle('BruniO · PC');self.resize(1280,820);self.setMinimumSize(900,640)
        self.setWindowIcon(QIcon(str(ASSETS/'brunio_icon.png')))
        self.client=Client();self.vault=Vault();self.local=self.vault.load();self.client.session=self.local.get('session',{})
        self.client.profile=self.local.get('profile','default');self.preferences=self.local.get('preferences',{'language':'es','audio':'es','sub':'es','limit':0,'autoplay':True})
        self.state={'favorites':[],'progress':{}};self.history=self.local.get('history',{});self.ratings={};self.selected=None;self.current=None;self.sources=[]
        self.pool=QThreadPool(self);self.pool.setMaxThreadCount(6);self.tasks=set();self.network=QNetworkAccessManager(self);self.images={};self.known_members=None
        self.setup_desktop_features();self.play_id=0;self.page_id=0;self.poll_busy=False;self.social_busy=False;self.sync_applying=False;self.guest_intent=None;self.guest_until=0;self.last_event=-1;self.paused_at=0;self.resume_pending=0;self.trakt_started=False
        self.vlc=None;self.player=None;self.library=None;self.segments={};self.imdb='';self.chat_dialog=None
        self.root=QWidget();self.root_layout=QVBoxLayout(self.root);self.root_layout.setContentsMargins(0,0,0,0);self.setCentralWidget(self.root)
        header=QWidget();header.setObjectName('topbar');self.sidebar=header
        nav=QHBoxLayout(header);self.nav=nav;nav.setContentsMargins(28,12,28,12);nav.setSpacing(8)
        mark=QLabel('Bruni<span style="color:#ab86ff">O</span>');mark.setTextFormat(Qt.TextFormat.RichText);mark.setObjectName('brand');nav.addWidget(mark)
        nav.addSpacing(22);self.nav_buttons={}
        for key,es,en,fn in [('home','Inicio','Home',self.home),('search','Buscar','Search',self.search),('live','Live TV','Live TV',self.live),('friends','Amigos','Friends',self.friends),('calendar','Calendario','Calendar',self.calendar),('downloads','Descargas','Downloads',self.downloads),('list','Mi lista','My List',self.my_list)]:
            b=button(self.t(es,en),lambda key=key,fn=fn:(self.select_nav(key),fn()));b.setObjectName('navButton');b.setToolTip(self.t(es,en));b.setAccessibleName(self.t(es,en));nav.addWidget(b);self.nav_buttons[key]=b
        nav.addStretch(1)
        self.alert_button=button('',self.alerts);self.alert_button.setObjectName('roundButton');self.alert_button.setIcon(icon('bell'));self.alert_button.setIconSize(QSize(20,20));self.alert_button.setToolTip(self.t('Notificaciones','Notifications'));nav.addWidget(self.alert_button)
        settings_button=button('',lambda:(self.select_nav('settings'),self.settings()));settings_button.setObjectName('roundButton');settings_button.setIcon(icon('settings'));settings_button.setIconSize(QSize(20,20));settings_button.setToolTip(self.t('Ajustes','Settings'));nav.addWidget(settings_button);self.nav_buttons['settings']=settings_button
        self.profile_button=button('',self.profiles);self.profile_button.setObjectName('profile');self.profile_button.setFixedSize(42,42);self.profile_button.setIconSize(QSize(34,34));nav.addWidget(self.profile_button)
        self.root_layout.addWidget(header);self.stack=QStackedWidget();self.root_layout.addWidget(self.stack,1);self.select_nav('home');self.refresh_profile_icon()
        self.status=QLabel();self.status.setWordWrap(True);self.status.setTextFormat(Qt.TextFormat.PlainText);self.status.setObjectName('status');self.status.hide();self.root_layout.addWidget(self.status)
        self.player_event.connect(self.on_player_event)
        self.tick_timer=QTimer(self);self.tick_timer.timeout.connect(self.tick);self.tick_timer.start(1000)
        self.social_timer=QTimer(self);self.social_timer.timeout.connect(self.social_tick);self.social_timer.start(5000)
        if smoke:self.show_page(column()[0]);return
        if self.client.session:self.run(self.client.load_account,lambda _:self.load_profile(),guard=False)
        else:self.settings()
    def select_nav(self,key):
        for name,b in self.nav_buttons.items():
            b.setProperty('selected',name==key);b.style().unpolish(b);b.style().polish(b)
    def refresh_profile_icon(self):
        profile=next((p for p in self.client.account.get('profiles',[]) if p.get('id')==self.client.profile),{})
        value=profile.get('avatarValue') or 'ai:astronaut';self.profile_button.setToolTip(profile.get('name',self.t('Perfiles','Profiles')))
        if value.startswith('http'):
            self.image(value,self.profile_button,38,38)
        else:self.profile_button.setIcon(QIcon(str(self.avatar_path(value))))
    def platform_rail(self,parent,page):
        scroll=QScrollArea();scroll.setWidgetResizable(True);scroll.setFixedHeight(132);scroll.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);scroll.setVerticalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff)
        header,hl=row();hl.addWidget(label(self.t('Plataformas','Streaming'),22),1)
        for key,direction in [('left',-1),('right',1)]:
            b=button('',lambda direction=direction:scroll.horizontalScrollBar().setValue(scroll.horizontalScrollBar().value()+direction*max(240,scroll.viewport().width()-240)));b.setIcon(icon(key));b.setFixedSize(34,34);b.setToolTip(self.t('Desplazar plataformas','Scroll platforms'));hl.addWidget(b)
        parent.addWidget(header);w,l=row();l.setSpacing(14);cards={}
        for name,pid,color in [('NETFLIX',8,'#e50914'),('Disney+',337,'#dce7ff'),('prime video',9,'#00a8e1'),('Apple TV+',350,'#ffffff'),('HBO Max',1899,'#cb6aff'),('AMC+',526,'#ffffff'),('Crunchyroll',283,'#f47521')]:
            card,cl=column();cl.setContentsMargins(0,0,0,0);cl.setSpacing(6);card.setFixedWidth(166)
            tile=button(name,lambda name=name,pid=pid:self.provider(name,pid));tile.setFixedSize(166,90);tile.setIconSize(QSize(110,70));tile.setStyleSheet(f'QPushButton{{background:#19191b;color:{color};font-size:20px;font-weight:700;text-align:center;border-radius:16px;}} QPushButton:hover,QPushButton:focus{{border:2px solid white;}}');cl.addWidget(tile);cl.addWidget(label(name,14));l.addWidget(card);cards[pid]=tile
        l.addStretch();scroll.setWidget(w);parent.addWidget(scroll)
        def logos(j):
            if page!=self.page_id:return
            for provider in j.get('results',[]):
                pid=provider.get('provider_id');path=provider.get('logo_path')
                if pid in cards and path:
                    self.image('https://image.tmdb.org/t/p/w154'+path,cards[pid],100,100)
        self.run(lambda:self.client.tmdb('/watch/providers/movie'),logos,quiet=True)
    def t(self,es,en):return es if self.preferences.get('language')=='es' else en
    def tell(self,text):
        self.status.setText(str(text));self.status.setVisible(bool(text))
        if text:QTimer.singleShot(6000,lambda text=str(text):self.status.hide() if isValid(self.status) and self.status.text()==text else None)
    def run(self,fn,done=lambda _:None,guard=True,quiet=False,finally_=None):
        context=self.client.context()
        def invoke():
            if guard:self.client.check(context)
            return fn()
        task=Task(invoke);self.tasks.add(task)
        task.signals.result.connect(lambda value:done(value) if not guard or context==self.client.context() else None)
        task.signals.error.connect(lambda text:self.tell(text) if not quiet and (not guard or context==self.client.context()) else None)
        def finished():
            self.tasks.discard(task)
            if finally_:finally_()
        task.signals.finished.connect(finished);self.pool.start(task)
    def persist(self):
        if os.name=='nt':self.vault.save({'session':self.client.session,'profile':self.client.profile,'preferences':self.preferences,'history':self.history})
    def show_page(self,w):
        if self.multi_page is not None:self.close_multiview()
        self.page_id+=1
        old=self.stack.currentWidget();self.stack.addWidget(w);self.stack.setCurrentWidget(w)
        if old and old is not getattr(self,'player_page',None):self.stack.removeWidget(old);old.deleteLater()
    def page(self,title):
        w,l=column();
        if title!='BruniO':l.addWidget(label(title,30))
        scroll=QScrollArea();scroll.setWidgetResizable(True);scroll.setFrameShape(QScrollArea.Shape.NoFrame);scroll.setWidget(w);self.show_page(scroll);return l,self.page_id
    def image(self,url,w,width,height):
        if not url:return
        def apply(data):
            if isValid(w):
                pix=QPixmap();pix.loadFromData(data);scaled=pix.scaled(width,height,Qt.AspectRatioMode.KeepAspectRatioByExpanding,Qt.TransformationMode.SmoothTransformation)
                if hasattr(w,'setPixmap'):w.setPixmap(scaled)
                else:
                    w.setIcon(QIcon(scaled))
                    if isinstance(w,QPushButton):w.setText('')
        if url in self.images:apply(self.images[url]);return
        req=QNetworkRequest(QUrl(url));req.setTransferTimeout(15000);reply=self.network.get(req)
        def finish():
            if reply.error()==reply.NetworkError.NoError:
                data=bytes(reply.readAll())
                if len(data)<8*1024*1024:
                    if len(self.images)>160:self.images.clear()
                    self.images[url]=data;apply(data)
            reply.deleteLater()
        reply.finished.connect(finish)
    def avatar_path(self,value):
        names={'ai:astronaut':'brunio_avatar_astronaut.png','ai:fox':'brunio_avatar_fox.png','ai:robot':'brunio_avatar_robot.png','ai:dragon':'brunio_avatar_dragon.png','special:photo':'brunio_avatar_special.png','special:extra':'brunio_avatar_extra.png','special:extra3':'brunio_avatar_special3.jpg'}
        if value.startswith('special:extra') and value[13:].isdigit() and int(value[13:])>=4:names[value]=f'brunio_avatar_special{value[13:]}.png'
        return ASSETS/names.get(value,'brunio_avatar_astronaut.png')
    def card(self,item):
        w,l=column();l.setContentsMargins(0,0,0,0);l.setSpacing(7);w.setFixedWidth(160)
        pic=Poster();pic.setFixedSize(160,236);pic.setCursor(Qt.CursorShape.PointingHandCursor)
        pic.setToolTip(item['title']+f" · {item.get('year','')} · ★ {item.get('rating',0):.1f}");pic.setAccessibleName(item['title'])
        pic.clicked.connect(lambda checked=False:self.open(item));self.image(item.get('poster',''),pic,154,230);l.addWidget(pic)
        title=QLabel();title.setTextFormat(Qt.TextFormat.PlainText);title.setText(title.fontMetrics().elidedText(item['title'],Qt.TextElideMode.ElideRight,156));title.setToolTip(item['title']);title.setStyleSheet('font-size:14px;color:#ededed;');l.addWidget(title)
        year=label(str(item.get('year','')),12);year.setStyleSheet('font-size:12px;color:#9b9fa6;');l.addWidget(year);return w
    def rail(self,parent,title,items):
        header,hl=row();hl.addWidget(label(title,22),1)
        scroll=QScrollArea();scroll.setWidgetResizable(True);scroll.setFixedHeight(304);scroll.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);scroll.setVerticalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff)
        for key,direction in [('left',-1),('right',1)]:
            b=button('',lambda direction=direction:scroll.horizontalScrollBar().setValue(scroll.horizontalScrollBar().value()+direction*max(300,scroll.viewport().width()-176)));b.setIcon(icon(key));b.setFixedSize(34,34);b.setToolTip(self.t('Desplazar fila','Scroll row'));hl.addWidget(b)
        parent.addWidget(header);w,l=row();l.setSpacing(14);l.setContentsMargins(0,4,0,12)
        for item in items:l.addWidget(self.card(item))
        l.addStretch();scroll.setWidget(w);parent.addWidget(scroll)
    def load_profile(self):
        self.client.language='es-ES' if self.preferences.get('language')=='es' else 'en-US'
        def load():return self.client.state(),self.client.rest('miflix_ratings?'+urlencode({'profile_id':'eq.'+self.client.profile,'select':'cloud_id,score','limit':1000}))
        def done(value):
            self.refresh_profile_icon();self.state=value[0];self.ratings={x['cloud_id']:x['score'] for x in value[1]};self.persist();self.home()
        self.run(load,done)
    def home(self):
        if not self.client.session:self.settings();return
        self.select_nav('home');l,page=self.page('BruniO');l.setSpacing(24)
        progress=dict(self.state.get('progress',{}))
        ids=[k for k,v in sorted(progress.items(),key=lambda x:x[1].get('updatedAt',0),reverse=True) if ':s' not in k and v.get('position',0)>=5 and v.get('percent',0)<90][:12]
        def load():
            paths=[('Tendencias','Trending','/trending/all/week',None),('Películas populares','Popular movies','/movie/popular','movie'),('Series populares','Popular series','/tv/popular','series'),('Mejor valoradas','Top rated','/movie/top_rated','movie'),('En cartelera','Now playing','/movie/now_playing','movie'),('Series destacadas','Top series','/tv/top_rated','series')]
            import concurrent.futures
            def section(entry):
                try:return self.t(entry[0],entry[1]),self.client.catalog(entry[2],entry[3])
                except Exception:return self.t(entry[0],entry[1]),[]
            def resume(key):
                try:return self.client.by_id(key)
                except Exception:return None
            with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
                rows=list(pool.map(section,paths));continued=[x for x in pool.map(resume,ids) if x]
            return rows,continued
        def done(value):
            if page!=self.page_id:return
            rows,continued=value
            if rows and rows[0][1]:
                hero=rows[0][1][0];art=Art(hero=True);art.setMinimumHeight(360);art.setMaximumHeight(400);al=QVBoxLayout(art);al.setContentsMargins(36,32,36,32);al.addStretch()
                badge=label(self.t('BRUNIO  ·  DESTACADO','BRUNIO  ·  FEATURED'),12);badge.setStyleSheet('background:transparent;color:#c4a6ff;font-size:12px;font-weight:700;letter-spacing:2px;');al.addWidget(badge)
                title=label(hero['title'],36);title.setMaximumWidth(650);title.setStyleSheet('background:transparent;font-size:36px;font-weight:700;');al.addWidget(title)
                meta=label(str(hero.get('year',''))+'   ·   ★ '+str(hero.get('rating',0)),13);meta.setStyleSheet('background:transparent;color:#d5d2dd;font-size:13px;');al.addWidget(meta)
                overview=label(hero['overview'][:230]+('…' if len(hero['overview'])>230 else ''),15);overview.setMaximumWidth(600);overview.setStyleSheet('background:transparent;color:#dddce4;font-size:15px;');al.addWidget(overview)
                actions,bl=row();actions.setObjectName('heroActions');actions.setStyleSheet('QWidget#heroActions { background:transparent; }');play=button(self.t('▶  Reproducir','▶  Play'),lambda:self.start(hero,1 if hero['type']=='series' else 0,1 if hero['type']=='series' else 0));play.setObjectName('primaryAction');bl.addWidget(play);bl.addWidget(button(self.t('Más información','More information'),lambda:self.open(hero)));bl.addStretch();al.addWidget(actions);self.image(hero.get('backdrop',''),art,1200,400);l.addWidget(art)
            if continued:self.rail(l,self.t('Seguir viendo','Continue watching'),continued)
            self.platform_rail(l,page)
            for title,items in rows:
                if items:self.rail(l,title,items)
            self.run(self.recommendations,lambda items:self.rail(l,self.t('Para ti','For you'),items) if page==self.page_id and items else None,quiet=True)
        self.run(load,done)
    def recommendations(self):
        seeds=[k for k,v in sorted(self.ratings.items(),key=lambda x:x[1],reverse=True) if v>=6]+self.state.get('favorites',[])
        items={}
        for cloud in list(dict.fromkeys(seeds))[:3]:
            m=self.client.by_id(cloud);kind='tv' if m['type']=='series' else 'movie'
            for item in self.client.catalog(f"/{kind}/{m['id']}/recommendations",m['type']):items[item['cloudId']]=item
        return list(items.values())
    def random_movie(self):
        if self.client.party and not self.client.host():self.tell(self.t('El host elige el contenido','The host chooses the title'));return
        def choose():
            rows=[x for x in self.recommendations() if x['type']=='movie'] or self.client.catalog('/movie/top_rated','movie')
            rows=[x for x in rows if self.ratings.get(x['cloudId'],10)>4 and self.state.get('progress',{}).get(x['cloudId'],{}).get('percent',0)<80]
            if not rows:raise ValueError('No available movies')
            return random.choice(rows)
        self.run(choose,lambda item:self.start(item,0,0))
    def grid(self,l,items):
        grid=QGridLayout();grid.setSpacing(16)
        columns=max(1,min(8,(self.width()-148)//174))
        for i,item in enumerate(items):grid.addWidget(self.card(item),i//columns,i%columns)
        l.addLayout(grid)
    def search(self):
        l,page=self.page(self.t('Buscar','Search'));q=QLineEdit();q.setPlaceholderText(self.t('Películas, series…','Movies, series…'));l.addWidget(q)
        owner=self.client.session.get('userId','')+':'+self.client.profile
        recent,rl=row()
        for previous in self.history.get(owner,[])[:10]:rl.addWidget(button(previous,lambda text=previous:(q.setText(text),submit())))
        rl.addStretch();recent_scroll=QScrollArea();recent_scroll.setWidgetResizable(True);recent_scroll.setFixedHeight(72);recent_scroll.setWidget(recent);l.addWidget(recent_scroll)
        results=QWidget();result_l=QVBoxLayout(results);l.addWidget(results)
        def submit():
            query=q.text().strip()
            if len(query)<2:return
            self.history[owner]=([query]+[old for old in self.history.get(owner,[]) if normalize(old)!=normalize(query)])[:10];self.persist()
            def found(rows):
                if page!=self.page_id:return
                while result_l.count():
                    child=result_l.takeAt(0)
                    if child.widget():child.widget().deleteLater()
                    if child.layout():
                        while child.layout().count():
                            x=child.layout().takeAt(0)
                            if x.widget():x.widget().deleteLater()
                self.grid(result_l,rows)
            self.run(lambda:self.client.search(query),found)
        q.returnPressed.connect(submit);l.addWidget(button(self.t('Buscar','Search'),submit));q.setFocus()
    def collections(self):
        l,page=self.page(self.t('Colecciones','Collections'))
        groups=[('Netflix',8),('Disney+',337),('Prime Video',9),('Apple TV+',350),('Max',1899)]
        for name,pid in groups:l.addWidget(button(name,lambda name=name,pid=pid:self.provider(name,pid)))
        for name,gid in [('Action',28),('Comedy',35),('Drama',18),('Horror',27),('Sci-Fi',878),('Thriller',53),('Animation',16)]:l.addWidget(button(name,lambda name=name,gid=gid:self.catalog_page(name,'/discover/movie','movie',{'with_genres':gid})))
        l.addWidget(button(self.t('Próximamente','Coming soon'),lambda:self.catalog_page('Coming soon','/discover/movie','movie',{'primary_release_date.gte':time.strftime('%Y-%m-%d')})))
        for year in range(time.localtime().tm_year,1999,-1):l.addWidget(button(str(year),lambda year=year:self.catalog_page(str(year),'/discover/movie','movie',{'primary_release_year':year})))
    def catalog_page(self,title,path,kind,params=None):
        l,page=self.page(title);self.run(lambda:self.client.catalog(path,kind,params),lambda items:self.grid(l,items) if page==self.page_id else None)
    def provider(self,title,pid):
        l,page=self.page(title)
        def load():
            base={'with_watch_providers':pid,'watch_region':'US'}
            rows=[(self.t('Películas','Movies'),self.client.catalog('/discover/movie','movie',base)),(self.t('Series','Series'),self.client.catalog('/discover/tv','series',base))]
            for genre,gid in [('Action',28),('Comedy',35),('Horror',27),('Drama',18),('Sci-Fi',878)]:rows.append((genre,self.client.catalog('/discover/movie','movie',{**base,'with_genres':gid})))
            return rows
        self.run(load,lambda rows:[self.rail(l,name,items) for name,items in rows] if page==self.page_id else None)
    def my_list(self):
        l,page=self.page(self.t('Mi lista','My list'));self.run(lambda:[self.client.by_id(x) for x in self.state.get('favorites',[])],lambda items:self.grid(l,items) if page==self.page_id else None)
    def open(self,item):
        self.selected=item;l,page=self.page(item['title']);pic=QLabel();pic.setFixedHeight(240);self.image(item.get('backdrop',''),pic,1000,240);l.addWidget(pic);l.addWidget(label(item['overview'],18));l.addWidget(label(f"{item['year']} · ★ {item['rating']:.1f}"))
        actions,al=row();play=button('▶ Play',lambda:self.start(item,1 if item['type']=='series' else 0,1 if item['type']=='series' else 0));al.addWidget(play);al.addWidget(button(self.t('Enlaces','Playback links'),lambda:self.start(item,1 if item['type']=='series' else 0,1 if item['type']=='series' else 0,manual=True)));al.addWidget(button('♥',lambda:self.favorite(item)));al.addWidget(button(self.t('Puntuar','Rate'),lambda:self.rate(item)));
        if item['type']=='movie':al.addWidget(button(self.t('Descargar','Download'),lambda:self.download_title(item)))
        l.addWidget(actions);play.setFocus()
        saved=self.state.get('progress',{}).get(item['cloudId'],{})
        if saved.get('position',0):l.addWidget(button(self.t('Continuar','Resume'),lambda:self.start(item,saved.get('season',0),saved.get('episode',0),position=int(saved['position']*1000))))
        if item['type']=='series':
            combo=QComboBox();l.addWidget(combo);l.addWidget(button(self.t('Descargar temporada','Download season'),lambda:self.download_season(item,combo.currentData()) if combo.currentData() is not None else None));episodes=QWidget();el=QVBoxLayout(episodes);l.addWidget(episodes)
            def season_changed():
                season=combo.currentData()
                if season is None:return
                def show(rows):
                    if page!=self.page_id:return
                    while el.count():
                        x=el.takeAt(0)
                        if x.widget():x.widget().deleteLater()
                    for ep in rows:
                        al=button(f"E{ep['episode_number']} · {ep.get('name','')}  ▶",lambda ep=ep:self.start(item,season,ep['episode_number']));al.setToolTip(ep.get('overview',''));el.addWidget(al)
                self.run(lambda:self.client.episodes(item,season),show)
            combo.currentIndexChanged.connect(season_changed)
            self.run(lambda:self.client.details(item),lambda d:[combo.addItem(f'Season {n}',n) for n in range(1,d.get('number_of_seasons',1)+1)] if page==self.page_id else None)
    def favorite(self,item):
        def save():
            state=self.client.state();fav=state.setdefault('favorites',[])
            if item['cloudId'] in fav:fav.remove(item['cloudId'])
            else:fav.append(item['cloudId'])
            self.client.save_state(state);return state
        self.run(save,lambda state:(setattr(self,'state',state),self.tell(self.t('Mi lista actualizada','My list updated'))))
    def rate(self,item):
        score,ok=QInputDialog.getInt(self,'BruniO',self.t('Puntuación 1–10','Rating 1–10'),self.ratings.get(item['cloudId'],8),1,10)
        if ok:self.run(lambda:self.client.rest('miflix_ratings?on_conflict=user_id,profile_id,cloud_id','POST',{'user_id':self.client.session['userId'],'profile_id':self.client.profile,'cloud_id':item['cloudId'],'score':score},'resolution=merge-duplicates,return=minimal'),lambda _:self.ratings.update({item['cloudId']:score}))
    def ensure_player(self):
        if self.player:return
        import vlc
        self.vlc=vlc;self.library=vlc.Instance('--no-video-title-show','--network-caching=1000','--avcodec-hw=any');self.player=self.library.media_player_new()
        for ev,text in [(vlc.EventType.MediaPlayerPlaying,'playing'),(vlc.EventType.MediaPlayerPaused,'paused'),(vlc.EventType.MediaPlayerEndReached,'ended'),(vlc.EventType.MediaPlayerEncounteredError,'error')]:self.player.event_manager().event_attach(ev,lambda event,text=text:self.player_event.emit(text))
        self.player_page,l=column();self.player_page.setObjectName('player');top,tl=row();self.title_label=label('',20);tl.addWidget(button('✕',self.stop_player));tl.addWidget(self.title_label,1);tl.addWidget(button('⛶',self.fullscreen));l.addWidget(top)
        self.video=QWidget();self.video.setAttribute(Qt.WidgetAttribute.WA_NativeWindow);self.video.setStyleSheet('background:black;');l.addWidget(self.video,1)
        self.pause_info=label('',16);self.pause_info.hide();l.addWidget(self.pause_info)
        self.seek=QSlider(Qt.Orientation.Horizontal);self.seek.setRange(0,1000);self.seek.sliderReleased.connect(self.seek_to);l.addWidget(self.seek)
        controls,cl=row();controls.setObjectName('playerControls');self.play_button=button('▶ / ❚❚',self.toggle_play);cl.addWidget(self.play_button)
        self.time_label=label('00:00 / 00:00');self.time_label.setStyleSheet('font-size:13px;color:#b6bac8;');cl.addWidget(self.time_label);cl.addStretch()
        cl.addWidget(button(self.t('Fuentes','Sources'),self.player_links))
        cl.addWidget(button(self.t('Audio / CC','Audio / CC'),self.subtitles))
        cl.addWidget(button(self.t('Llenar','Fill'),self.fit))
        cl.addWidget(button(self.t('Opciones','Options'),self.player_options))
        l.addWidget(controls)
        self.stack.addWidget(self.player_page)
        if os.name=='nt':self.player.set_hwnd(int(self.video.winId()))
        else:self.player.set_xwindow(int(self.video.winId()))
    def start(self,item,season=0,episode=0,manual=False,position=0,remote=False):
        if item['type']=='live':
            if self.current:self.resolve_live(item,self.live_manifest('sports' if item.get('liveProvider')=='sports' else 'channels'))
            return
        if self.client.party and not self.client.host() and not remote:
            state=self.client.party['state']
            if (item['cloudId'],season,episode)!=(state.get('cloudId'),state.get('season',0),state.get('episode',0)):self.tell('The host chooses the title');return
        self.play_id+=1;ticket=self.play_id;self.tell(self.t('Buscando enlaces…','Finding playback links…'))
        def done(value):
            if ticket!=self.play_id:return
            streams,imdb=value
            if not streams:self.remote_loading=None;self.tell(self.t('No hay enlaces compatibles con tus filtros','No playback links match your filters'));return
            self.sources=streams;self.imdb=imdb
            if manual:self.source_dialog(item,season,episode,position,streams,ticket)
            else:self.play_stream(item,season,episode,position,streams[0],imdb)
        self.run(lambda:self.client.streams(item,season,episode,self.preferences.get('limit',0)),done)
    def source_dialog(self,item,season,episode,position,streams,ticket):
        dialog=QDialog(self);dialog.setWindowTitle('Playback links');dialog.resize(760,560);l=QVBoxLayout(dialog);scroll=QScrollArea();scroll.setWidgetResizable(True);w,sl=column();scroll.setWidget(w);l.addWidget(scroll)
        for s in streams:
            text=f"{s.get('name','Stream')} · {stream_quality(s)}p · {stream_size(s)/10**9:.2f} GB\n{s.get('title','')}\n{(s.get('behaviorHints') or {}).get('filename','')}"
            def choose(s=s):
                dialog.accept()
                if ticket==self.play_id:self.play_stream(item,season,episode,position,s,self.imdb)
            line,line_layout=row();b=button(text,choose);b.setMinimumHeight(82);line_layout.addWidget(b,1)
            from .downloads import file_extension
            if file_extension(s):line_layout.addWidget(button(self.t('Descargar','Download'),lambda s=s:(dialog.accept(),self._start_download(item,season,episode,s))))
            sl.addWidget(line)
        l.addWidget(button(self.t('Cancelar','Cancel'),dialog.reject));dialog.exec()
    def play_stream(self,item,season,episode,position,stream,imdb=''):
        self.ensure_player();self.player.stop();self.remote_loading=None;self.segments={};self.imdb=imdb
        self.current={'item':item,'season':season,'episode':episode,'stream':stream};self.resume_pending=position;self.title_label.setText(item['title']+(f' · S{season} E{episode}' if season else ''));self.pause_info.hide();self.paused_at=0
        m=self.library.media_new(stream['url']);m.add_option(':audio-language='+self.preferences.get('audio','es'));m.add_option(':sub-language='+('none' if self.preferences.get('sub')=='off' else self.preferences.get('sub','es')))
        hints=stream.get('behaviorHints',{}).get('proxyHeaders',{}).get('request',{})
        for header,opt in [('User-Agent','http-user-agent'),('Referer','http-referrer')]:
            if hints.get(header):m.add_option(':'+opt+'='+hints[header])
        if position:m.add_option(':start-time='+str(position/1000))
        self.player.set_media(m);self.stack.setCurrentWidget(self.player_page);self.player.play();m.release();self.tell(item['title'])
        ticket=self.play_id
        if season:
            def episode_info(rows):
                if ticket!=self.play_id:return
                ep=next((e for e in rows if e['episode_number']==episode),{})
                if self.current:self.current['episodeOverview']=ep.get('overview',item.get('overview',''))
            self.run(lambda:self.client.episodes(item,season),episode_info,quiet=True)
        if imdb:
            self.run(lambda:self.client.transport('https://api.introdb.app/segments?'+urlencode({'imdb_id':imdb,**({'season':season,'episode':episode} if season else {'is_movie':'true'})})),lambda j:setattr(self,'segments',j) if ticket==self.play_id else None,quiet=True)
    def player_options(self):
        d=QDialog(self);d.setWindowTitle(self.t('Opciones del reproductor','Player options'));d.resize(420,520);layout=QVBoxLayout(d)
        for name,fn in [(self.t('Audio','Audio'),self.audio),(self.t('Subtítulos','Subtitles'),self.subtitles),
                        (self.t('Episodios','Episodes'),self.player_episodes),('Watch Party',self.party_controls),
                        (self.t('Velocidad','Speed'),self.speed),('Skip intro',lambda:self.skip_segment('intro')),
                        ('Skip credits',lambda:self.skip_segment('outro')),(self.t('Pantalla completa','Fullscreen'),self.fullscreen)]:
            layout.addWidget(button(name,lambda fn=fn:(d.accept(),fn())))
        if self.current and self.current['item']['type']=='live':
            layout.addWidget(button('MultiView',lambda:(d.accept(),self.open_multiview())))
            layout.addWidget(button('Live Stats',lambda:(d.accept(),self.sports_scores())))
        elif self.current:
            c=dict(self.current)
            from .downloads import file_extension
            if file_extension(c['stream']):layout.addWidget(button(self.t('Descargar','Download'),lambda:(d.accept(),self._start_download(c['item'],c['season'],c['episode'],c['stream']))))
        d.exec()
    def toggle_play(self):
        if not self.player or not self.current:return
        self.pause_info.hide();self.paused_at=time.monotonic()
        desired=not bool(self.player.is_playing());self.player.set_pause(0 if desired else 1)
        if self.client.party and not self.client.host():
            self.guest_intent=desired;self.guest_until=time.monotonic()+10;c=dict(self.current)
            self.run(lambda:self.client.request_playback(c['item'],c['season'],c['episode'],desired),quiet=False)
    def seek_to(self):
        if self.player and self.player.get_length()>0:self.player.set_time(int(self.seek.value()/1000*self.player.get_length()));self.pause_info.hide()
    def fit(self):
        if self.player:self.player.video_set_aspect_ratio('16:9' if not getattr(self,'stretched',False) else None);self.stretched=not getattr(self,'stretched',False)
    def speed(self):
        value,ok=QInputDialog.getItem(self,'Speed','Speed',['0.75','1.0','1.25','1.5','2.0'],1,False)
        if ok and self.player:self.player.set_rate(float(value))
    def audio(self):
        if not self.player:return
        tracks=self.player.audio_get_track_description() or []
        labels=[str(name.decode(errors='replace') if isinstance(name,bytes) else name) for _,name in tracks]
        value,ok=QInputDialog.getItem(self,'Audio','Audio track',labels,0,False)
        if ok:self.player.audio_set_track(tracks[labels.index(value)][0])
    def subtitles(self):
        if not self.player:return
        dialog=QDialog(self);dialog.setWindowTitle('Subtitles');l=QVBoxLayout(dialog)
        for track,name in self.player.video_get_spu_description() or []:l.addWidget(button(name.decode(errors='replace') if isinstance(name,bytes) else str(name),lambda track=track:(self.player.video_set_spu(track),dialog.accept())))
        l.addWidget(button(self.t('Abrir archivo SRT/VTT','Open SRT/VTT file'),lambda:self.subtitle_file(dialog)))
        if self.imdb:l.addWidget(button('OpenSubtitles EN / ES',lambda:self.external_subs(dialog)))
        dialog.exec()
    def subtitle_file(self,dialog):
        path,_=QFileDialog.getOpenFileName(self,'Subtitle','','Subtitles (*.srt *.vtt *.ass)')
        if path:self.player.add_slave(self.vlc.MediaSlaveType.subtitle,QUrl.fromLocalFile(path).toString(),True);dialog.accept()
    def external_subs(self,dialog):
        vid=self.imdb+(f":{self.current['season']}:{self.current['episode']}" if self.current['season'] else '');ticket=self.play_id
        def done(j):
            if ticket!=self.play_id:return
            choices=[x for x in j.get('subtitles',[]) if x.get('lang') in ['es','spa','en','eng'] and valid_url(x.get('url',''))]
            if not choices:self.tell('No EN/ES subtitles found');return
            text,ok=QInputDialog.getItem(self,'OpenSubtitles','Subtitles',[x.get('lang','')+' · '+x.get('label',str(i)) for i,x in enumerate(choices)],0,False)
            if ok:
                index=[x.get('lang','')+' · '+x.get('label',str(i)) for i,x in enumerate(choices)].index(text);self.player.add_slave(self.vlc.MediaSlaveType.subtitle,choices[index]['url'],True)
        dialog.accept();self.run(lambda:self.client.transport(f'https://opensubtitles-v3.strem.io/subtitles/{self.current["item"]["type"]}/{vid}/*.json'),done)
    def player_links(self):
        if self.current:
            c=self.current;self.start(c['item'],c['season'],c['episode'],True,max(0,self.player.get_time()))
    def player_episodes(self):
        if not self.current or self.current['item']['type']!='series':return
        c=dict(self.current);d=QDialog(self);d.setWindowTitle('Episodes');d.resize(600,500);l=QVBoxLayout(d);seasons=QComboBox();l.addWidget(seasons);scroll=QScrollArea();scroll.setWidgetResizable(True);w,el=column();scroll.setWidget(w);l.addWidget(scroll)
        def changed():
            season=seasons.currentData()
            if season is None:return
            def show(rows):
                if not isValid(d):return
                while el.count():
                    child=el.takeAt(0)
                    if child.widget():child.widget().deleteLater()
                for ep in rows:
                    def play(ep=ep):d.accept();self.start(c['item'],season,ep['episode_number'])
                    el.addWidget(button(f"E{ep['episode_number']} · {ep.get('name','')}",play))
            self.run(lambda:self.client.episodes(c['item'],season),show)
        seasons.currentIndexChanged.connect(changed)
        self.run(lambda:self.client.details(c['item']),lambda j:[seasons.addItem(f'Season {n}',n) for n in range(1,j.get('number_of_seasons',1)+1)] if isValid(d) else None)
        d.exec()
    def skip_segment(self,key):
        s=self.segments.get(key)
        if s and s.get('end_ms',0)>s.get('start_ms',0):self.player.set_time(s['end_ms'])
    def fullscreen(self):self.showNormal() if self.isFullScreen() else self.showFullScreen()
    def on_player_event(self,event):
        if event=='error':self.tell(self.t('No se pudo reproducir. Prueba otro enlace.','Playback failed. Try another link.'))
        if event=='ended' and self.current:
            if self.client.party and not self.client.host():return
            c=dict(self.current)
            if c['item']['type']=='series' and self.preferences.get('autoplay',True):self.next_episode(c)
            else:self.open(c['item']);self.run(lambda:self.client.catalog(f"/{'tv' if c['item']['type']=='series' else 'movie'}/{c['item']['id']}/recommendations",c['item']['type']),lambda rows:self.show_recommendations(rows))
    def show_recommendations(self,rows):
        l,page=self.page(self.t('Algo más para ver','More to watch'));self.grid(l,rows)
    def next_episode(self,c):
        def find():
            eps=self.client.episodes(c['item'],c['season']);today=time.strftime('%Y-%m-%d')
            for e in eps:
                if e['episode_number']>c['episode'] and e.get('air_date') and e['air_date']<=today:return c['season'],e['episode_number']
            seasons=self.client.details(c['item']).get('number_of_seasons',0)
            if c['season']<seasons:
                for e in self.client.episodes(c['item'],c['season']+1):
                    if e.get('air_date') and e['air_date']<=today:return c['season']+1,e['episode_number']
            return None
        self.run(find,lambda ep:self.start(c['item'],*ep) if ep else self.open(c['item']))
    def stop_player(self):
        self.play_id+=1
        if self.player:self.player.stop()
        item=self.current['item'] if self.current else None;self.current=None
        if item and item['type']!='live':self.open(item)
        else:self.home()
    def tick(self):
        if self.player and self.current:
            pos=max(0,self.player.get_time());duration=max(0,self.player.get_length());playing=bool(self.player.is_playing())
            self.play_button.setText('⏸' if playing else '▶')
            if duration and not self.seek.isSliderDown():self.seek.setValue(int(pos/duration*1000))
            self.time_label.setText(f'{pos//60000}:{pos//1000%60:02} / {duration//60000}:{duration//1000%60:02}')
            if not playing and self.player.get_state()==self.vlc.State.Paused:
                if not self.paused_at:self.paused_at=time.monotonic()
                if time.monotonic()-self.paused_at>=5:
                    self.pause_info.setText(time.strftime('%H:%M')+f" · {(duration-pos)//60000} min left\n"+self.current.get('episodeOverview',self.current['item'].get('overview','')));self.pause_info.show()
            else:self.paused_at=0;self.pause_info.hide()
            if duration and int(time.time())%20==0:self.save_progress(pos,duration)
        if not self.client.session or self.poll_busy:return
        if self.client.pending_room and not self.client.party:
            self.poll_busy=True;self.run(lambda:self.client.party_join(self.client.pending_room),lambda decision:self.joined(decision),guard=False,quiet=True,finally_=lambda:setattr(self,'poll_busy',False));return
        if not self.client.party:return
        self.poll_busy=True;room=self.client.party['room_code'];c=dict(self.current) if self.current else None
        pos=max(0,self.player.get_time()) if self.player else 0;playing=bool(self.player and self.player.is_playing())
        def poll():
            if self.client.host():
                desired=self.client.social_rpc('rp1_miflix_take_playback',{'code':room})
                if c:
                    state={'cloudId':c['item']['cloudId'],'season':c['season'],'episode':c['episode'],'positionMs':pos,'playing':desired if isinstance(desired,bool) else playing,'updatedAt':now_ms(),'liveChannelId':c['item'].get('liveChannelId',''),'liveProvider':c['item'].get('liveProvider',''),'liveTitle':c['item']['title'] if c['item']['type']=='live' else '', 'liveType':c['item'].get('liveType','tv')}
                    self.client.host_update(state)
                return desired,None
            return None,self.client.party_read(room)
        def done(result):
            desired,p=result
            if self.client.host():
                if isinstance(desired,bool) and self.player:self.player.set_pause(0 if desired else 1)
                return
            if p is None:self.leave_party();self.tell('Room closed or expired');return
            self.client.party=p;s=p['state'];key=(s.get('cloudId'),s.get('season',0),s.get('episode',0))
            current_key=(self.current['item']['cloudId'],self.current['season'],self.current['episode']) if self.current else None
            if not s.get('cloudId'):return
            if key!=current_key:
                if getattr(self,'remote_loading',None)==key and time.monotonic()<getattr(self,'remote_deadline',0):return
                self.remote_loading=key;self.remote_deadline=time.monotonic()+30;ticket=self.play_id
                def item_done(item):
                    self.start(item,s.get('season',0),s.get('episode',0),position=s.get('positionMs',0),remote=True)
                if s.get('liveChannelId'):self.remote_live(s)
                else:self.run(lambda:self.client.by_id(s['cloudId']),item_done)
                return
            if self.guest_intent==s.get('playing') or time.monotonic()>self.guest_until:self.guest_intent=None
            if self.guest_intent is None and self.player:
                if s.get('playing') and abs(max(0,self.player.get_time())-s.get('positionMs',0))>1800:self.player.set_time(s.get('positionMs',0))
                self.player.set_pause(0 if s.get('playing') else 1)
        self.run(poll,done,quiet=True,finally_=lambda:setattr(self,'poll_busy',False))
    def save_progress(self,pos,duration):
        c=self.current
        if not c or c['item']['type']=='live':return
        def save():
            state=self.client.state();p={'percent':pos/duration*100,'position':pos/1000,'duration':duration/1000,'updatedAt':now_ms(),'season':c['season'],'episode':c['episode']};key=c['item']['cloudId']
            state.setdefault('progress',{})[key]=p
            if c['season']:state['progress'][key+f":s{c['season']}e{c['episode']}"]=p
            self.client.save_state(state);return state
        self.run(save,lambda state:setattr(self,'state',state),quiet=True)
        if pos/duration>=0.8 and c['item']['type'] in ('movie','series'):
            key=c['item']['cloudId']+f":{c['season']}:{c['episode']}"
            sent=self.preferences.setdefault('trakt_sent',[])
            if key not in sent:
                sent.append(key);event='pc_'+str(uuid.uuid4())
                self.run(lambda:self.client.trakt('watched',tmdb=c['item']['id'],type=c['item']['type'],season=c['season'],episode=c['episode'],event=event),quiet=True)
    def party_controls(self):
        dialog=QDialog(self);dialog.setWindowTitle('Watch Party');l=QVBoxLayout(dialog)
        if self.client.party:
            room=self.client.party['room_code'];l.addWidget(label('Party · '+room,24));l.addWidget(button('QR / Mobile chat',lambda:self.qr('https://randy3m2.github.io/miflix-tv-native/party/?room='+room)))
            l.addWidget(button('Chat / Reactions',self.chat));l.addWidget(button(self.t('Salir de Party','Leave Party'),lambda:(dialog.accept(),self.leave_party())))
        else:
            l.addWidget(button(self.t('Crear Party','Create Party'),lambda:(dialog.accept(),self.run(self.client.party_create,lambda _:self.party_created(),guard=False))))
            code=QLineEdit();code.setPlaceholderText('123456');code.setMaxLength(6);l.addWidget(code)
            l.addWidget(button(self.t('Solicitar acceso','Request access'),lambda room=code:(dialog.accept(),self.run(lambda value=room.text().strip():self.client.party_join(value),self.joined,guard=False))))
        dialog.exec()
    def party_created(self):
        self.known_members={self.client.social_identity()};self.last_event=-1;self.tell('Party · '+self.client.party['room_code']);self.party_controls()
    def joined(self,decision):
        if decision=='approved':self.last_event=-1;self.tell(self.t('Te uniste a la sala','Joined the room'))
        elif decision=='pending':self.tell(self.t('Esperando aprobación del host','Waiting for host approval'))
        else:self.tell(self.t('Solicitud rechazada','Request declined'))
    def leave_party(self):
        self.play_id+=1;self.remote_loading=None
        if self.player:self.player.stop()
        self.current=None;self.known_members=None;self.last_event=-1;self.guest_intent=None
        snapshot=(self.client.party,dict(self.client.session));self.client.detach()
        self.run(lambda:self.client.party_leave(snapshot),lambda _:self.home())
    def friends(self):
        l,page=self.page(self.t('Amigos / Party','Friends / Party'));l.addWidget(button('Party',self.party_controls));l.addWidget(button(self.t('Actualizar','Refresh'),self.friends))
        l.addWidget(button(self.t('Mi apodo / frases','My nickname / phrases'),self.social_settings));l.addWidget(button(self.t('Añadir amigo por apodo','Add friend by nickname'),self.add_friend))
        def show(value):
            if page!=self.page_id:return
            activity,pending=value
            for person in activity:
                w,rl=row();rl.addWidget(label('@'+person['nickname']+' · '+(person.get('title') or ('Online' if person.get('online') else 'Offline')),20))
                if person.get('room_code'):rl.addWidget(button('Request access',lambda p=person:self.run(lambda:self.client.party_join(p['room_code']),self.joined,guard=False)))
                l.addWidget(w)
            for f in pending:
                if f['receiver_id']==self.client.social_identity() and f['status']=='pending':l.addWidget(button('Accept friend · '+f['sender_id'][:8],lambda f=f:self.run(lambda:self.client.social_rest('rp1_miflix_friendships?'+urlencode({'sender_id':'eq.'+f['sender_id'],'receiver_id':'eq.'+self.client.social_identity()}),'PATCH',{'status':'accepted'}),lambda _:self.friends())))
        self.run(lambda:(self.client.social_rpc('rp1_miflix_friend_activity',{}),self.client.social_rest('rp1_miflix_friendships?select=sender_id,receiver_id,status')),show)
    def add_friend(self):
        nickname,ok=QInputDialog.getText(self,'Friends','Nickname')
        if not ok:return
        def add():
            rows=self.client.social_rest('rp1_miflix_social_profiles?'+urlencode({'nickname':'eq.'+nickname.strip().lower(),'select':'user_id'}))
            if not rows:raise ValueError('Nickname not found')
            if rows[0]['user_id']==self.client.social_identity():raise ValueError('This is your account')
            self.client.social_rest('rp1_miflix_friendships','POST',{'sender_id':self.client.social_identity(),'receiver_id':rows[0]['user_id']})
        self.run(add,lambda _:self.tell('Friend request sent'))
    def social_settings(self):
        d=QDialog(self);d.setWindowTitle('Nickname / Phrases');l=QVBoxLayout(d);nick=QLineEdit();nick.setPlaceholderText('nickname');l.addWidget(nick)
        def save_nick():
            import re
            text=nick.text().strip().lower()
            if not re.fullmatch('[a-z0-9_]{3,24}',text):self.tell('Use 3–24 letters, numbers or underscores');return
            self.run(lambda:self.client.social_rest('rp1_miflix_social_profiles?on_conflict=user_id','POST',{'user_id':self.client.social_identity(),'nickname':text},'resolution=merge-duplicates,return=minimal'),lambda _:self.tell('Nickname saved'))
        l.addWidget(button('Save nickname',save_nick));phrases=QLineEdit('; '.join(self.preferences.get('phrases',['¡Qué escena!','No puede ser 😂','🍿'])));l.addWidget(label('Phrases · separate with ;'));l.addWidget(phrases)
        def save_phrases():self.preferences['phrases']=[x.strip()[:400] for x in phrases.text().split(';') if x.strip()][:10];self.persist();d.accept()
        l.addWidget(button('Save phrases',save_phrases));d.exec()
    def send_event(self,kind,text):
        if not self.client.party:return
        self.run(lambda:self.client.social_rest('rp1_miflix_party_events','POST',{'room_code':self.client.party['room_code'],'user_id':self.client.social_identity(),'kind':kind,'body':text.strip()[:400]}),quiet=True)
    def chat(self):
        if not self.client.party:return
        d=QDialog(self);d.setWindowTitle('Live chat · '+self.client.party['room_code']);d.resize(520,520);l=QVBoxLayout(d);self.chat_log=label('',16);l.addWidget(self.chat_log,1);text=QLineEdit();text.setMaxLength(400);l.addWidget(text)
        def send():self.send_event('chat',text.text());text.clear()
        text.returnPressed.connect(send);l.addWidget(button('Send',send));w,rl=row()
        for emoji in ['😂','❤️','🔥','😱','👏','🍿']:rl.addWidget(button(emoji,lambda emoji=emoji:self.send_event('emoji',emoji)))
        l.addWidget(w)
        for phrase in self.preferences.get('phrases',[]):l.addWidget(button(phrase,lambda phrase=phrase:self.send_event('phrase',phrase)))
        self.chat_dialog=d;d.exec();self.chat_dialog=None
    def social_tick(self):
        if self.client.session and time.monotonic()>getattr(self,'notice_after',0):
            self.notice_after=time.monotonic()+1800;self.refresh_notices()
        if not self.client.session or self.social_busy:return
        self.social_busy=True;item=self.current['item'] if self.current else None;season=self.current['season'] if self.current else 0;episode=self.current['episode'] if self.current else 0
        def poll():
            self.client.presence(item,season,episode);requests=self.client.social_rpc('rp1_miflix_access_requests',{}) or []
            members=[];events=[]
            if self.client.party:
                room=self.client.party['room_code'];members=self.client.social_rest('rp1_miflix_party_members?'+urlencode({'room_code':'eq.'+room,'select':'user_id'})) or []
                params={'room_code':'eq.'+room,'select':'id,user_id,kind,body','order':'id.desc' if self.last_event<0 else 'id.asc','limit':60}
                if self.last_event>=0:params['id']='gt.'+str(self.last_event)
                events=self.client.social_rest('rp1_miflix_party_events?'+urlencode(params)) or []
                if members:
                    ids=[m['user_id'] for m in members]
                    names=self.client.social_rest('rp1_miflix_social_profiles?'+urlencode({'user_id':'in.('+','.join(ids)+')','select':'user_id,nickname'})) or []
                    lookup={p['user_id']:p['nickname'] for p in names}
                    members=[{**m,'nickname':lookup.get(m['user_id'],'Guest')} for m in members]
            return requests,members,events
        def done(value):
            requests,members,events=value
            for request in requests:
                if getattr(self,'request_dialog',False):break
                self.request_dialog=True
                try:
                    approve=QMessageBox.question(self,'Party',request['nickname']+' · Request access',QMessageBox.StandardButton.Yes|QMessageBox.StandardButton.No)==QMessageBox.StandardButton.Yes
                    self.run(lambda r=request,a=approve:self.client.social_rpc('rp1_miflix_decide_access',{'code':r['room_code'],'guest':r['user_id'],'approve':a}))
                finally:self.request_dialog=False
            ids={m['user_id'] for m in members}
            if self.client.host() and self.known_members is not None:
                added=ids-self.known_members
                if added:self.tell(' · Joined: '+', '.join(m.get('nickname','Guest') for m in members if m['user_id'] in added))
            self.known_members=ids
            for ev in sorted(events,key=lambda x:x['id']):
                if self.last_event>=0 and ev['id']>self.last_event:self.tell(ev['body'])
                self.last_event=max(self.last_event,ev['id'])
                if self.chat_dialog and isValid(self.chat_log):self.chat_log.setText((self.chat_log.text()+'\n'+ev['body'])[-6000:])
            self.persist()
        self.run(poll,done,quiet=True,finally_=lambda:setattr(self,'social_busy',False))
    def qr(self,url):
        import qrcode
        import io
        data=io.BytesIO();qrcode.make(url).save(data,format='PNG');pix=QPixmap();pix.loadFromData(data.getvalue());d=QDialog(self);d.setWindowTitle('BruniO · QR');l=QVBoxLayout(d);image=QLabel();image.setPixmap(pix.scaled(300,300));l.addWidget(image);l.addWidget(button('Open on this PC',lambda:webbrowser.open(url)));d.exec()
    def profiles(self):
        l,page=self.page(self.t('¿Quién está viendo?','Who is watching?'))
        for profile in self.client.account.get('profiles',[]):
            w,rl=row();pic=QLabel();pic.setFixedSize(88,88);value=profile.get('avatarValue') or 'ai:astronaut'
            if value.startswith('https://'):self.image(value,pic,88,88)
            elif value!='none':pic.setPixmap(QPixmap(str(self.avatar_path(value))).scaled(88,88))
            rl.addWidget(pic);rl.addWidget(button(profile['name'],lambda p=profile:self.select_profile(p)));rl.addWidget(button('Avatar',lambda p=profile:self.avatar_picker(p)))
            if len(self.client.account['profiles'])>1:rl.addWidget(button(self.t('Eliminar perfil','Delete profile'),lambda p=profile:self.delete_profile(p)))
            l.addWidget(w)
        l.addWidget(button(self.t('Crear perfil','Create profile'),self.create_profile))
    def select_profile(self,profile):
        if self.client.party:self.tell('Leave Party before switching profiles');return
        if self.player:self.player.stop()
        self.current=None;self.client.detach();self.client.profile=profile['id'];self.load_profile()
    def create_profile(self):
        name,ok=QInputDialog.getText(self,'Profiles',self.t('Nombre del perfil','Profile name'))
        if ok and name.strip():
            profiles=self.client.account['profiles']+[{'id':str(uuid.uuid4()),'name':name.strip()[:24],'avatarValue':'ai:astronaut','primary':False}];self.run(lambda:self.client.profiles(profiles),lambda _:self.profiles())
    def delete_profile(self,p):
        if QMessageBox.question(self,'Profiles','Delete '+p['name']+'?')!=QMessageBox.StandardButton.Yes:return
        def done(rows):
            self.client.account['profiles']=rows
            if self.client.profile==p['id']:self.select_profile(rows[0])
            else:self.profiles()
        self.run(lambda:self.client.rpc('miflix_delete_profile',{'target_profile':p['id']}),done)
    def avatar_picker(self,p):
        d=QDialog(self);d.setWindowTitle('Avatar');d.resize(640,500);outer=QVBoxLayout(d);scroll=QScrollArea();scroll.setWidgetResizable(True);w,l=column();scroll.setWidget(w);outer.addWidget(scroll);grid=QGridLayout();l.addLayout(grid)
        keys=['ai:astronaut','ai:fox','ai:robot','ai:dragon','special:photo','special:extra']+['special:extra'+str(x) for x in range(3,16)]
        def choose(key):
            profiles=[{**x,'avatarValue':key} if x['id']==p['id'] else x for x in self.client.account['profiles']]
            d.accept();self.run(lambda:self.client.profiles(profiles),lambda _:self.profiles())
        for i,key in enumerate(keys):
            b=button(key,lambda key=key:choose(key));b.setIcon(QIcon(str(self.avatar_path(key))));b.setIconSize(__import__('PySide6.QtCore',fromlist=['QSize']).QSize(72,72));grid.addWidget(b,i//3,i%3)
        outer.addWidget(button('Remove photo',lambda:choose('none')));outer.addWidget(button('Upload photo from this PC',lambda:self.upload_avatar(p,d)));d.exec()
    def upload_avatar(self,p,dialog):
        path,_=QFileDialog.getOpenFileName(self,'Avatar','','Images (*.jpg *.jpeg *.png *.webp)')
        if not path:return
        from PIL import Image,ImageOps
        import io
        with Image.open(path) as source:picture=ImageOps.fit(ImageOps.exif_transpose(source).convert('RGB'),(256,256))
        data=io.BytesIO();picture.save(data,format='JPEG',quality=88);raw=data.getvalue();dialog.accept()
        def upload():
            from urllib.request import Request,urlopen
            object_path=self.client.session['userId']+'/'+p['id']+'/'+str(uuid.uuid4())+'.jpg'
            with urlopen(Request(SUPABASE_URL+'/storage/v1/object/miflix-avatars/'+object_path,data=raw,method='POST',headers={'apikey':SUPABASE_KEY,'Authorization':'Bearer '+self.client.session['accessToken'],'Content-Type':'image/jpeg'}),timeout=25):pass
            url=SUPABASE_URL+'/storage/v1/object/public/miflix-avatars/'+object_path
            self.client.profiles([{**x,'avatarValue':url} if x['id']==p['id'] else x for x in self.client.account['profiles']])
        self.run(upload,lambda _:self.profiles())
    def live_manifest(self,provider):return self.preferences.get('sports_manifest') or LIVE['sports'] if provider=='sports' else LIVE['channels']
    def live(self):
        l,page=self.page('Live TV');l.addWidget(button(self.t('Marcadores y estadísticas','Scores and stats'),self.sports_scores))
        owner=self.client.session.get('userId','local')+':'+self.client.profile
        provider=QComboBox();provider.addItem('Live channels','channels');provider.addItem('Live sports','sports');provider.addItem('IPTV · M3U','m3u');l.addWidget(provider)
        category=QComboBox();l.addWidget(category);search=QLineEdit();search.setText(self.preferences.get('live_search',''));search.setPlaceholderText(self.t('Buscar canales','Search channels'));l.addWidget(search)
        holder=QWidget();hl=QVBoxLayout(holder);l.addWidget(holder);playlist={'rows':[]}
        def import_playlist():
            path,_=QFileDialog.getOpenFileName(self,self.t('Abrir lista M3U','Open M3U playlist'),'','M3U (*.m3u *.m3u8);;All files (*)')
            if not path:return
            self.preferences.setdefault('playlists',{})[owner]=path;self.persist();provider.setCurrentIndex(provider.findData('m3u'));categories()
        l.addWidget(button(self.t('Importar lista M3U','Import M3U playlist'),import_playlist))
        def categories():
            if provider.currentData()=='m3u':
                path=self.preferences.get('playlists',{}).get(owner,'')
                if not path:self.tell(self.t('Importa una lista M3U.','Import an M3U playlist.'));return
                from .playlist import read_m3u
                def done(rows):
                    if page!=self.page_id:return
                    playlist['rows']=rows;category.clear();category.addItem(self.t('Todos','All'),'*');category.addItem(self.t('Favoritos','Favorites'),'favorites')
                    for value in sorted(set(x.get('category','Others') for x in rows),key=str.casefold):category.addItem(value,value)
                self.run(lambda:read_m3u(path),done);return
            def done(j):
                if page!=self.page_id:return
                category.clear()
                for cat in j.get('catalogs',[]):
                    category.addItem(cat.get('name',cat['id']),cat)
                    for extra in cat.get('extra',[]):
                        if extra['name']=='genre':
                            for genre in extra.get('options',[]):category.addItem(genre,{**cat,'genre':genre})
            self.run(lambda:self.client.transport(self.live_manifest(provider.currentData())),done)
        def show(rows,provider_key,manifest):
            if page!=self.page_id:return
            while hl.count():
                child=hl.takeAt(0)
                if child.widget():child.widget().deleteLater()
            self.live_candidates=[(channel,provider_key,manifest) for channel in rows if channel.get('id')]
            favorites=self.preferences.setdefault('live_favorites',{}).setdefault(owner,[])
            for index,channel in enumerate(rows,1):
                if normalize(search.text()) not in normalize(channel.get('name','')):continue
                line,bl=row();identifier=str(channel.get('id',''))
                def play(c=channel,p=provider_key,m=manifest):
                    if p=='m3u':
                        if self.client.party:self.tell(self.t('Los canales M3U locales no se comparten en Party.','Local M3U channels cannot be shared in Party.'));return
                        item={'id':0,'type':'live','cloudId':'tmdb:live:'+str(java_hash(c['id'])),'title':c.get('name','Channel'),'overview':'','liveChannelId':c['id'],'liveProvider':'custom','liveType':'tv'}
                        self.play_id+=1;self.play_stream(item,0,0,0,{'url':c['url'],'name':c['name']})
                    else:self.play_channel(c,p,m)
                bl.addWidget(button(f"{index:03} · {channel.get('name','Channel')}",play),1)
                def toggle(id=identifier):
                    if id in favorites:favorites.remove(id)
                    else:favorites.append(id)
                    self.persist();channels()
                bl.addWidget(button('★' if identifier in favorites else '☆',toggle));hl.addWidget(line)
        def channels():
            cat=category.currentData()
            if cat is None:return
            self.preferences['live_search']=search.text();self.persist();provider_key=provider.currentData()
            if provider_key=='m3u':
                favorites=self.preferences.setdefault('live_favorites',{}).setdefault(owner,[])
                rows=[x for x in playlist['rows'] if cat=='*' or (cat=='favorites' and x['id'] in favorites) or x.get('category')==cat]
                show(rows,provider_key,'');return
            manifest=self.live_manifest(provider_key);query='genre='+quote(cat['genre']) if cat.get('genre') else ''
            url=manifest.removesuffix('/manifest.json')+f"/catalog/{quote(cat.get('type','tv'))}/{quote(cat['id'])}"+('/'+query if query else '')+'.json'
            self.run(lambda:self.client.transport(url),lambda j:show(j.get('metas',[]),provider_key,manifest))
        provider.currentIndexChanged.connect(categories);category.currentIndexChanged.connect(channels);search.returnPressed.connect(channels);l.addWidget(button(self.t('Buscar','Search'),channels));categories()
    def play_channel(self,channel,provider,manifest):
        if self.client.party and not self.client.host():self.tell('The host chooses the channel');return
        item={'id':0,'type':'live','cloudId':'tmdb:live:'+str(java_hash(channel['id'])),'title':channel.get('name','Channel'),'overview':channel.get('description',''),'liveChannelId':channel['id'],'liveProvider':'sports' if provider=='sports' else 'nauta','liveType':channel.get('type','tv')}
        self.resolve_live(item,manifest)
    def resolve_live(self,item,manifest):
        self.play_id+=1;ticket=self.play_id
        def done(j):
            rows=[x for x in j.get('streams',[]) if valid_url(x.get('url',''))]
            if ticket!=self.play_id:return
            if not rows:self.tell('No playable channel links');return
            self.play_stream(item,0,0,0,rows[0])
        self.run(lambda:self.client.transport(manifest.removesuffix('/manifest.json')+f"/stream/{quote(item['liveType'])}/{quote(item['liveChannelId'],safe='')}.json"),done)
    def remote_live(self,s):
        provider='sports' if s.get('liveProvider')=='sports' else 'channels';item={'id':0,'type':'live','cloudId':s['cloudId'],'title':s.get('liveTitle','Live'),'overview':'','liveChannelId':s['liveChannelId'],'liveProvider':s.get('liveProvider',''),'liveType':s.get('liveType','tv')};self.resolve_live(item,self.live_manifest(provider))
    def refresh_notices(self,done=None):
        owner=self.client.session.get('userId','')+':'+self.client.profile
        old=dict(self.preferences.setdefault('known_releases',{}).get(owner,{}));favorites=list(self.state.get('favorites',[]))
        def load():
            updates=[];known=dict(old)
            for cloud in favorites[:40]:
                item=self.client.by_id(cloud);d=self.client.details(item);episode=d.get('last_episode_to_air')
                key=str(episode.get('id')) if episode else d.get('release_date','')
                date=episode.get('air_date','') if episode else d.get('release_date','')
                if date and date<=time.strftime('%Y-%m-%d'):
                    if cloud in known and known[cloud]!=key:updates.append(item['title']+' · '+date)
                    known[cloud]=key
            return known,updates
        def received(value):
            known,updates=value;self.preferences['known_releases'][owner]=known
            notices=self.preferences.setdefault('notifications',{}).setdefault(owner,[])
            for text in updates:
                if text not in notices:notices.insert(0,text)
            del notices[100:];self.alert_button.setText(''+(' '+str(len(notices))+'+' if notices else ''));self.persist()
            if done:done(notices)
        self.run(load,received,quiet=True)
    def alerts(self):
        l,page=self.page(self.t('Notificaciones','Notifications'));owner=self.client.session.get('userId','')+':'+self.client.profile
        def clear():self.preferences.setdefault('notifications',{})[owner]=[];self.alert_button.setText('');self.persist();self.alerts()
        l.addWidget(button(self.t('Borrar notificaciones','Clear notifications'),clear))
        def shown(rows):
            if page!=self.page_id:return
            for text in rows:l.addWidget(label(text,20))
            if not rows:l.addWidget(label(self.t('No hay notificaciones','No notifications')))
        shown(self.preferences.get('notifications',{}).get(owner,[]))
        self.refresh_notices(lambda _:None)
    def settings(self,refresh=True):
        self.select_nav('settings')
        if refresh and self.client.session:
            l,page=self.page(self.t('Ajustes','Settings'));l.addWidget(label(self.t('Sincronizando configuración de la cuenta…','Syncing account settings…')))
            self.run(self.client.load_account,lambda _:(self.refresh_profile_icon(),self.settings(refresh=False)) if page==self.page_id else None);return
        l,page=self.page('Settings · BruniO '+VERSION)
        if not self.client.session:
            email=QLineEdit();email.setPlaceholderText('Email');password=QLineEdit();password.setPlaceholderText('Password');password.setEchoMode(QLineEdit.EchoMode.Password);l.addWidget(email);l.addWidget(password)
            def login():
                e=email.text().strip();p=password.text();password.clear();self.run(lambda:self.client.login(e,p),lambda _:self.load_profile(),guard=False)
            l.addWidget(button(self.t('Iniciar sesión','Sign in'),login));password.returnPressed.connect(login)
            l.addWidget(button(self.t('Acceder con QR','QR sign-in'),self.qr_login))
            l.addWidget(label(self.t('Usa la misma cuenta de BruniO TV. Cada persona usa su propia cuenta y complementos.','Use your BruniO TV account. Each person uses their own account and add-ons.')));return
        l.addWidget(label(self.client.session.get('email',''),18));token=QLineEdit(self.client.token);token.setEchoMode(QLineEdit.EchoMode.Password);token.setPlaceholderText('TMDB token');l.addWidget(token)
        addon=QLineEdit();addon.setPlaceholderText('https://…/manifest.json');addon.setEchoMode(QLineEdit.EchoMode.Password);l.addWidget(addon)
        l.addWidget(label(self.t('Tus complementos','Your add-ons')+' · '+str(len(self.client.manifests))))
        def setup():
            manifests=list(self.client.manifests);url=addon.text().strip()
            if url:
                if not valid_url(url):self.tell('Invalid manifest URL');return
                manifests=list(dict.fromkeys(manifests+[url if url.endswith('/manifest.json') else url.rstrip('/')+'/manifest.json']))
            self.run(lambda token_value=token.text().strip(),new=url:self.client.edit_setup(token=token_value,add=(new if new.endswith('/manifest.json') else new.rstrip('/')+'/manifest.json') if new else None),lambda _:(self.tell(self.t('Configuración compartida con TV guardada','Shared TV account setup saved')),self.settings()))
        l.addWidget(button('Save account setup / Add add-on',setup))
        for manifest in self.client.manifests:
            host=__import__('urllib.parse',fromlist=['urlsplit']).urlsplit(manifest).hostname
            l.addWidget(button('Remove '+str(host),lambda m=manifest:self.run(lambda:self.client.edit_setup(remove=m),lambda _:self.settings())))
        sports=QLineEdit(self.preferences.get('sports_manifest',''));sports.setPlaceholderText('Live sports manifest URL (optional)');sports.setEchoMode(QLineEdit.EchoMode.Password);l.addWidget(sports)
        def save_sports():
            url=sports.text().strip()
            if url and not valid_url(url):self.tell('Invalid manifest URL');return
            self.save_pref('sports_manifest',url);self.tell('Live sports setup saved')
        l.addWidget(button('Save live sports setup',save_sports))
        for name,key,choices in [('Language','language',['es','en']),('Default audio','audio',['es','en','pt','fr','de','ja']),('Default subtitles','sub',['es','en','pt','fr','de','off'])]:
            combo=QComboBox();combo.addItems(choices);combo.setCurrentText(self.preferences.get(key,choices[0]));l.addWidget(label(name));l.addWidget(combo);combo.currentTextChanged.connect(lambda text,key=key:self.save_pref(key,text))
        limit=QDoubleSpinBox();limit.setRange(0,500);limit.setSuffix(' GB');limit.setValue(self.preferences.get('limit',0));limit.setSpecialValueText('Unlimited');l.addWidget(label('Size Limit'));l.addWidget(limit);limit.valueChanged.connect(lambda value:self.save_pref('limit',value))
        autoplay=QCheckBox('Autoplay next episode');autoplay.setChecked(self.preferences.get('autoplay',True));autoplay.toggled.connect(lambda value:self.save_pref('autoplay',value));l.addWidget(autoplay)
        l.addWidget(button('Clear image cache',lambda:(self.images.clear(),self.tell('Cache cleared'))));l.addWidget(button('Trakt',self.trakt));l.addWidget(button('Check for updates',self.check_updates));l.addWidget(button('Sign out',self.sign_out))
    def qr_login(self):
        from .pairing import Pairing
        import qrcode,io
        pairing=Pairing(self.client.transport);d=QDialog(self);d.setWindowTitle('BruniO · QR sign-in');l=QVBoxLayout(d);image=QLabel();l.addWidget(image);info=label('Preparing QR…');l.addWidget(info);timer=QTimer(d);busy=[False];active=[True]
        def ready(url):
            if not active[0] or not isValid(d):return
            buffer=io.BytesIO();qrcode.make(url).save(buffer,format='PNG');pix=QPixmap();pix.loadFromData(buffer.getvalue());image.setPixmap(pix.scaled(300,300));info.setText('Scan with your phone · 10 minutes');timer.start(2000)
        def received(payload):
            if not active[0] or not payload:return
            timer.stop()
            if payload.get('accessToken') and payload.get('userId'):
                self.client.detach();self.client.session={'accessToken':payload['accessToken'],'refreshToken':payload.get('refreshToken',''),'userId':payload['userId'],'email':payload.get('email','')}
                def loaded(_):
                    addon=payload.get('addonManifest','')
                    if addon:
                        self.run(lambda:self.client.edit_setup(add=addon),lambda _:self.load_profile())
                    else:self.load_profile()
                d.accept();self.run(self.client.load_account,loaded,guard=False)
            else:info.setText('Use the Account section on your phone to sign in')
            self.run(pairing.finish,quiet=True)
        def poll():
            if busy[0]:return
            busy[0]=True;self.run(pairing.poll,received,finally_=lambda:busy.__setitem__(0,False))
        timer.timeout.connect(poll);self.run(pairing.create,ready);d.exec();active[0]=False;timer.stop();self.run(pairing.finish,quiet=True)

    def save_pref(self,key,value):self.preferences[key]=value;self.client.language='es-ES' if self.preferences.get('language')=='es' else 'en-US';self.persist()
    def trakt(self):
        d=QDialog(self);d.setWindowTitle('Trakt');l=QVBoxLayout(d)
        def started(j):
            url=j.get('verification_uri') or j.get('verification_url')
            if url:self.qr(url)
            elif j.get('authorize_url'):webbrowser.open(j['authorize_url'])
            self.tell(j.get('user_code') or 'Authorize Trakt in your browser')
        l.addWidget(button('Connect Trakt',lambda:self.run(lambda:self.client.trakt('start'),started)))
        l.addWidget(button('Confirm authorization',lambda:self.run(lambda:self.client.trakt('poll'),lambda j:self.tell('Connected' if j.get('connected') else 'Waiting for authorization'))))
        def imported(j):
            for entry in j.get('items',[]):
                movie=entry.get('movie');show=entry.get('show');value=movie or show or {};tid=value.get('ids',{}).get('tmdb')
                if tid:
                    cloud=f"tmdb:{'movie' if movie else 'series'}:{tid}"
                    if cloud not in self.state.setdefault('favorites',[]):self.state['favorites'].append(cloud)
            self.run(lambda:self.client.save_state(self.state),lambda _:self.tell('Trakt watchlist imported'))
        l.addWidget(button('Import watchlist',lambda:self.run(lambda:self.client.trakt('watchlist'),imported)));l.addWidget(button('Disconnect Trakt',lambda:self.run(lambda:self.client.trakt('disconnect'),lambda _:self.tell('Trakt disconnected'))));d.exec()
    def check_updates(self):
        from .updater import check,download
        def done(info):
            if not info:self.tell('You have the latest PC version');return
            if QMessageBox.question(self,'BruniO','Download '+info['version']+'?')!=QMessageBox.StandardButton.Yes:return
            def downloaded(path):
                if QMessageBox.question(self,'BruniO','Installer verified. Close BruniO and install?')==QMessageBox.StandardButton.Yes:
                    os.startfile(str(path));self.close()
            self.tell('Downloading update…');self.run(lambda:download(info),downloaded)
        self.run(check,done)
    def sign_out(self):
        if self.player:self.player.stop()
        self.current=None;self.play_id+=1;self.client.detach();self.client.session={};self.client.account={};self.client.manifests=[];self.client.token='';self.vault.clear();self.state={'favorites':[],'progress':{}};self.ratings={};self.settings()
    def keyPressEvent(self,event):
        if event.key()==Qt.Key.Key_F11:self.fullscreen();return
        if event.key()==Qt.Key.Key_Escape and self.isFullScreen():self.showNormal();return
        if self.player and self.stack.currentWidget() is self.player_page and not isinstance(self.focusWidget(),QLineEdit):
            self.pause_info.hide();self.paused_at=time.monotonic()
            if event.key()==Qt.Key.Key_Space:self.toggle_play();return
            if event.key() in (Qt.Key.Key_Left,Qt.Key.Key_Right):self.player.set_time(max(0,self.player.get_time()+(10000 if event.key()==Qt.Key.Key_Right else -10000)));return
        super().keyPressEvent(event)
    def closeEvent(self,event):
        self.download_stop.set();self.tick_timer.stop();self.social_timer.stop();self.play_id+=1
        self.close_multiview()
        if self.player:self.player.stop();self.player.release();self.library.release()
        self.client.detach();self.persist();event.accept()

STYLE='''
QWidget { background:#080a10; color:#f5f5fa; font-family:"Segoe UI"; font-size:14px; }
QWidget#topbar { background:#10121b; border-bottom:1px solid #272b3a; }
QLabel#brand { color:#f7f7fb; font-size:27px; font-weight:800; letter-spacing:-1px; background:transparent; }
QPushButton { background:#202432; border:1px solid #33384b; border-radius:10px; padding:10px 15px; font-weight:600; }
QPushButton:hover { background:#30364a; border-color:#72608f; }
QPushButton:focus { border:2px solid #b094fa; }
QPushButton:pressed { background:#4d3d70; }
QPushButton#primaryAction { background:#f5f2ff; color:#161320; border:0; font-weight:700; padding:12px 22px; }
QPushButton#primaryAction:hover { background:#dacbff; }
QPushButton#navButton { background:transparent; color:#acb1c0; border:0; padding:10px 12px; font-size:13px; }
QPushButton#navButton:hover { background:#232332; color:#fff; }
QPushButton#navButton[selected="true"] { color:#fff; background:#332947; border-radius:9px; }
QPushButton#roundButton,QPushButton#profile { background:#1d202d; border:1px solid #363a4c; border-radius:20px; padding:7px; min-width:26px; }
QLabel#status { padding:9px 28px; color:#e7dcff; background:#2d2540; }
QLabel#muted { color:#a8acbc; }
QWidget#downloadCard { background:#171a25; border:1px solid #2f3445; border-radius:14px; }
QWidget#multiTile { background:#11131a; border:1px solid #35394b; border-radius:12px; }
QLineEdit,QComboBox,QDoubleSpinBox { padding:11px; border:1px solid #373b4c; border-radius:10px; background:#191c27; selection-background-color:#8364bd; }
QLineEdit:focus,QComboBox:focus { border:1px solid #a788e0; }
QScrollArea { border:0; background:#080a10; }
QSlider::groove:horizontal { height:4px; background:#414657; border-radius:2px; }
QSlider::sub-page:horizontal { background:#b997ff; border-radius:2px; }
QSlider::handle:horizontal { width:14px; margin:-5px 0; background:#f7f5ff; border-radius:7px; }
QScrollBar:vertical { width:7px; background:#080a10; }
QScrollBar::handle:vertical,QScrollBar::handle:horizontal { background:#44475a; border-radius:3px; min-height:24px; }
QScrollBar::add-line,QScrollBar::sub-line { height:0; width:0; }
QScrollBar:horizontal { height:7px; background:#080a10; }
QDialog { background:#10131c; }
'''



def main(smoke=False,smoke_vlc=False):
    app=QApplication(sys.argv);app.setStyleSheet(STYLE);window=Window(smoke);window.show()
    if smoke_vlc:window.ensure_player()
    if smoke:QTimer.singleShot(200,app.quit)
    return app.exec()
