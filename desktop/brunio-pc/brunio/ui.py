from __future__ import annotations
import json, os, random, sys, time, uuid, webbrowser, hashlib
from pathlib import Path
from urllib.parse import urlencode,quote
from PySide6.QtCore import Qt,QObject,Signal,QRunnable,QThreadPool,QTimer,QUrl,QSize
from PySide6.QtGui import QPixmap,QIcon,QImage,QPainter,QColor
from PySide6.QtNetwork import QNetworkAccessManager,QNetworkRequest
from PySide6.QtWidgets import (QApplication,QMainWindow,QWidget,QVBoxLayout,QHBoxLayout,QPushButton,QLabel,QLineEdit,QScrollArea,QStackedWidget,QDialog,QComboBox,QSlider,QInputDialog,QMessageBox,QFileDialog,QCheckBox,QDoubleSpinBox,QGridLayout,QSizePolicy,QListWidget,QButtonGroup,QGraphicsDropShadowEffect)
from shiboken6 import isValid
from .core import Client,StaleWork,SUPABASE_URL,SUPABASE_KEY,LIVE,REPO,iso,now_ms,media,stream_size,stream_quality,normalize,valid_url
from .vault import Vault
from .visuals import Art,Poster,ProfileAvatar,EdgeRail,Starfield,PlatformTile,ArtworkTile,ContinueCard,icon
from .desktop_features import DesktopFeatures

def java_hash(value):
    h=0
    encoded=value.encode('utf-16-be')
    for i in range(0,len(encoded),2):h=(31*h+int.from_bytes(encoded[i:i+2],'big'))&0xffffffff
    return h if h<2**31 else h-2**32
from . import VERSION

ASSETS=Path(getattr(sys,'_MEIPASS',Path(__file__).resolve().parent.parent))/'assets'
PLATFORM_ASSETS=ASSETS/'platforms'
if not PLATFORM_ASSETS.exists():PLATFORM_ASSETS=Path(__file__).resolve().parents[3]/'web'/'assets'/'platforms'
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
    w=QWidget();w.setObjectName('flatRow');layout=QHBoxLayout(w);layout.setContentsMargins(0,0,0,0);layout.setSpacing(12);return w,layout
def column():
    w=QWidget();layout=QVBoxLayout(w);layout.setContentsMargins(24,20,24,28);layout.setSpacing(16);return w,layout
def label(text,size=16):
    w=QLabel(text);w.setWordWrap(True);w.setStyleSheet(f'font-size:{size}px;');w.setTextFormat(Qt.TextFormat.PlainText);return w

def button(text,fn):
    b=QPushButton(text);b.setObjectName('pillButton');b.setCursor(Qt.CursorShape.PointingHandCursor);b.clicked.connect(lambda checked=False:fn());return b
class Window(DesktopFeatures,QMainWindow):
    player_event=Signal(str)
    def __init__(self,smoke=False):
        super().__init__();self.setWindowTitle('BruniO · PC');self.resize(1280,820);self.setMinimumSize(900,640)
        self.setWindowIcon(QIcon(str(ASSETS/'brunio_icon.png')))
        self.client=Client();self.vault=Vault();self.local=self.vault.load();self.client.session=self.local.get('session',{})
        self.client.profile=self.local.get('profile','default');self.preferences=self.local.get('preferences',{'language':'es','audio':'es','sub':'es','limit':0,'autoplay':True,'theme':'dark'})
        self.apply_theme()
        self.state={'favorites':[],'progress':{}};self.history=self.local.get('history',{});self.ratings={};self.selected=None;self.current=None;self.sources=[]
        self.pool=QThreadPool(self);self.pool.setMaxThreadCount(6);self.tasks=set();self.network=QNetworkAccessManager(self);self.images={};self.pending_images={}
        self.image_queue=[];self.image_running=0
        self._home_screen=None;self._home_owner=None;self._home_ready=False;self._home_loaded_at=0
        self._genre_art={};self._genre_art_requested=False
        self._catalog_cache={};self._catalog_lock=__import__('threading').Lock()
        self.image_cache_dir=Path(os.getenv('LOCALAPPDATA') or (Path.home()/'.cache'))/'BruniO-PC'/'artwork'
        try:
            self.image_cache_dir.mkdir(parents=True,exist_ok=True)
            data=json.loads((self.image_cache_dir/'genre-art.json').read_text(encoding='utf-8'))
            if time.time()-float(data.get('created',0))<14*86400:
                self._genre_art={k:v for k,v in data.get('images',{}).items()
                                 if isinstance(k,str) and isinstance(v,str)
                                 and v.startswith('https://image.tmdb.org/')}
        except (OSError,ValueError,TypeError,KeyError):pass
        self.known_members=None
        self.setup_desktop_features();self.play_id=0;self.page_id=0;self.poll_busy=False;self.social_busy=False;self.sync_applying=False;self.guest_intent=None;self.guest_until=0;self.last_event=-1;self.paused_at=0;self.resume_pending=0;self.trakt_started=False
        self.fallback_urls=set();self.source_activity=time.monotonic();self.source_last_pos=0;self.vlc=None;self.player=None;self.library=None;self.segments={};self.imdb='';self.chat_dialog=None
        # RC6: desktop-first two-column shell.  The Windows client keeps all
        # existing routes/controllers; only their Qt presentation is replaced.
        self.root=QWidget();self.root.setObjectName('appRoot')
        self.root_layout=QHBoxLayout(self.root)
        self.root_layout.setContentsMargins(0,0,0,0);self.root_layout.setSpacing(0)
        self.setCentralWidget(self.root)
        self.sidebar=QWidget();self.sidebar.setObjectName('sidebar');self.sidebar.setFixedWidth(226)
        self.nav=QVBoxLayout(self.sidebar);self.nav.setContentsMargins(15,28,15,18);self.nav.setSpacing(6)
        brand=QLabel('Bruni<span style="color:#b4a1ff">O</span>')
        brand.setTextFormat(Qt.TextFormat.RichText);brand.setObjectName('brand')
        self.nav.addWidget(brand);self.nav.addSpacing(26)
        self.nav_buttons={};self.nav_glyphs={}
        routes=[('home','home','Inicio','Home',self.home),
                ('search','search','Búsqueda','Search',self.search),
                ('list','list','Mi Lista','My List',self.my_list),
                ('movies','movies','Películas','Movies',lambda:self.catalog_page(self.t('Películas','Movies'),'/movie/popular','movie')),
                ('series','series','Series','Series',lambda:self.catalog_page(self.t('Series','Series'),'/tv/popular','series')),
                ('collections','collections','Explorar','Explore',self.collections),
                ('live','live','Live TV','Live TV',self.live),
                ('friends','friends','Amigos','Friends',self.friends),
                ('calendar','calendar','Calendario','Calendar',self.calendar),
                ('downloads','downloads','Descargas','Downloads',self.downloads)]
        self.nav_labels={key:(es,en) for key,_,es,en,_ in routes}
        # Six primary destinations match the reference; all other destinations stay available.
        self.secondary_routes={'friends','calendar','downloads'}
        self.extra_nav=QWidget();self.extra_nav.setObjectName('navExtras')
        extra_layout=QVBoxLayout(self.extra_nav)
        extra_layout.setContentsMargins(8,0,0,0);extra_layout.setSpacing(3)
        for key,glyph,es,en,fn in routes:
            b=button(self.t(es,en),lambda key=key,fn=fn:(self.select_nav(key),fn()))
            b.setObjectName('navButton');b.setIcon(icon(glyph,'#c5c6df'))
            b.setIconSize(QSize(20,20));b.setFixedHeight(44)
            b.setCursor(Qt.CursorShape.PointingHandCursor)
            b.setToolTip(self.t(es,en));b.setAccessibleName(self.t(es,en))
            if key in self.secondary_routes:extra_layout.addWidget(b)
            else:self.nav.addWidget(b)
            self.nav_buttons[key]=b;self.nav_glyphs[key]=glyph
        self.more_button=button(self.t('Más','More')+'  ⌄',self.toggle_more)
        self.more_button.setObjectName('navButton')
        self.more_button.setIcon(icon('collections','#c5c6df'))
        self.more_button.setIconSize(QSize(20,20));self.more_button.setFixedHeight(43)
        self.nav.addWidget(self.more_button)
        self.nav.addWidget(self.extra_nav);self.extra_nav.hide()
        self.nav.addStretch(1)
        self.nav.addWidget(QLabel('────────────────'),0,Qt.AlignmentFlag.AlignHCenter)
        settings_button=button(self.t('Ajustes','Settings'),lambda:(self.select_nav('settings'),self.settings()))
        settings_button.setObjectName('navButton');settings_button.setIcon(icon('settings','#b9b8d5'))
        settings_button.setIconSize(QSize(20,20));settings_button.setFixedHeight(44)
        self.nav.addWidget(settings_button)
        self.nav_buttons['settings']=settings_button;self.nav_glyphs['settings']='settings'
        self.alert_button=button(self.t('Notificaciones','Notifications'),self.alerts)
        self.alert_button.setObjectName('navButton')
        self.alert_button.setIcon(icon('bell','#b9b8d5'));self.alert_button.setIconSize(QSize(19,19))
        self.alert_button.setFixedHeight(44)
        self.nav.addWidget(self.alert_button)
        self.profile_button=ProfileAvatar();self.profile_button.setObjectName('profileAvatarButton')
        self.profile_button.clicked.connect(lambda:self.profiles())
        self.profile_button.setFixedSize(48,48)
        self.profile_button.setToolTip(self.t('Cambiar perfil','Switch profile'))
        profile_row,pr=row();profile_row.setObjectName('profileFooter');pr.setContentsMargins(7,8,7,8);pr.setSpacing(10)
        pr.addWidget(self.profile_button)
        self.profile_name_label=QLabel(self.t('Mi perfil','My profile'))
        self.profile_name_label.setObjectName('profileDisplayName')
        subtitle=QLabel(self.t('Cambiar perfil  ›','Switch profile  ›'))
        subtitle.setObjectName('profileFootnote')
        text_group,tx=column();tx.setContentsMargins(0,0,0,0);tx.setSpacing(2)
        tx.addWidget(self.profile_name_label);tx.addWidget(subtitle)
        pr.addWidget(text_group,1);self.nav.addWidget(profile_row)
        self.root_layout.addWidget(self.sidebar)
        self.content=QWidget();self.content.setObjectName('contentShell')
        content_layout=QVBoxLayout(self.content);content_layout.setContentsMargins(0,0,0,0)
        content_layout.setSpacing(0)
        self.stack=QStackedWidget();content_layout.addWidget(self.stack,1)
        self.status=QLabel();self.status.setWordWrap(True)
        self.status.setTextFormat(Qt.TextFormat.PlainText)
        self.status.setObjectName('status');self.status.hide();content_layout.addWidget(self.status)
        self.root_layout.addWidget(self.content,1)
        self.select_nav('home');self.refresh_profile_icon()
        QTimer.singleShot(9000,self.prune_image_cache)
        self.player_event.connect(self.on_player_event)
        self.tick_timer=QTimer(self);self.tick_timer.timeout.connect(self.tick);self.tick_timer.start(1000)
        self.social_timer=QTimer(self);self.social_timer.timeout.connect(self.social_tick);self.social_timer.start(5000)
        if smoke:self.show_page(column()[0]);return
        if self.client.session:self.run(self.client.load_account,lambda _:(self.refresh_profile_icon(),self.profiles()),guard=False)
        else:self.settings()
    def toggle_more(self):
        visible=not self.extra_nav.isHidden()
        self.extra_nav.setVisible(not visible)
        self.more_button.setText(self.t('Más','More')+('  ⌄' if visible else '  ⌃'))
        self.more_button.setProperty('selected',not visible)
        self.more_button.style().unpolish(self.more_button);self.more_button.style().polish(self.more_button)
    def select_nav(self,key):
        if key in self.secondary_routes and self.extra_nav.isHidden():self.toggle_more()
        for name,b in self.nav_buttons.items():
            active=name==key
            b.setProperty('selected',active)
            if name in self.nav_glyphs:
                b.setIcon(icon(self.nav_glyphs[name],'#f4efff' if active else '#989fbb'))
            b.style().unpolish(b);b.style().polish(b)
    def refresh_profile_icon(self):
        profile=next((p for p in self.client.account.get('profiles',[]) if p.get('id')==self.client.profile),{})
        value=profile.get('avatarValue') or 'ai:astronaut'
        display=profile.get('name') or self.t('Mi perfil','My profile')
        self.profile_button.setToolTip(display)
        self.profile_name_label.setText(display)
        if value.startswith('http'):
            self.image(value,self.profile_button,38,38)
        else:self.profile_button.setPixmap(QPixmap(str(self.avatar_path(value))))
    def section_title(self,text,subtitle=None):
        block,vertical=column();block.setObjectName('sectionHeader')
        vertical.setContentsMargins(0,0,0,0);vertical.setSpacing(2)
        headline=label(text,19);headline.setObjectName('sectionTitle');vertical.addWidget(headline)
        if subtitle:
            small=label(subtitle,12);small.setObjectName('muted');vertical.addWidget(small)
        return block
    def filter_pills(self,parent,labels,on_change,initial='all'):
        bar,layout=row();bar.setObjectName('filterBar');layout.setSpacing(8)
        chips={}
        for key,es,en in labels:
            pill=button(self.t(es,en),lambda key=key:on_change(key))
            pill.setObjectName('filterPill');pill.setProperty('selected',key==initial)
            pill.setFixedHeight(33);layout.addWidget(pill);chips[key]=pill
        layout.addStretch(1);parent.addWidget(bar)
        def select(key):
            for k,pill in chips.items():
                if not isValid(pill):continue
                pill.setProperty('selected',k==key)
                pill.style().unpolish(pill);pill.style().polish(pill)
        return select
    def platform_rail(self,parent,page):
        parent.addWidget(self.section_title(self.t('Tus servicios en BruniO','Your services on BruniO')))
        w,l=row();l.setSpacing(13)
        platforms=[('Netflix',8,'netflix.png'),('Disney+',337,'disney-plus.png'),
                   ('HBO Max',1899,'hbo-max.png'),('Prime Video',9,'prime-video.png'),
                   ('Apple TV+',350,'apple-tv.png'),('YouTube',None,None),
                   ('Paramount+',531,'paramount-plus.png'),('AMC+',526,'amcplus.png'),
                   ('Hulu',15,'hulu.png'),('Crunchyroll',283,'crunchyroll.png')]
        for name,pid,asset in platforms:
            tile=PlatformTile(name);tile.setFixedSize(218,122)
            tile.setToolTip(name);tile.setAccessibleName(name)
            if pid is not None:tile.clicked.connect(lambda checked=False,name=name,pid=pid:self.provider(name,pid))
            else:tile.clicked.connect(lambda:self.tell(self.t('YouTube no tiene todavía una colección TMDB.','YouTube does not have a TMDB collection yet.')))
            file=PLATFORM_ASSETS/asset if asset else None
            animation=PLATFORM_ASSETS/((asset or '').removesuffix('.png')+'.gif') if asset else None
            # Static brand art is visible immediately; GIF plays only on hover.
            if file and file.is_file():tile.setPixmap(QPixmap(str(file)))
            if animation and animation.is_file():tile.setAnimatedLogo(animation)
            elif name=='AMC+':tile.setPixmap(QPixmap(str(ASSETS/'streaming_amcplus_rp48.png')))
            elif name in ('Hulu','Paramount+'):tile.setPixmap(QPixmap(str(ASSETS/(name.lower().replace('+','-plus')+'.png'))))
            l.addWidget(tile)
        l.addStretch();parent.addWidget(EdgeRail(w,140))

    def continue_rail(self,parent,items):
        """Continue Watching uses cinematic backdrops and real resume progress."""
        parent.addWidget(self.section_title(self.t('Continuar viendo','Continue watching')))
        w,l=row();l.setSpacing(12);l.setContentsMargins(0,4,0,10)
        progress=self.state.get('progress',{})
        for item in items:
            saved=progress.get(item.get('cloudId',''),{})
            tile=ContinueCard(item.get('title',''),saved.get('percent',0))
            tile.setFixedSize(276,154);tile.setToolTip(item.get('title',''))
            tile.clicked.connect(lambda checked=False,item=item,saved=saved:self.start(
                item,saved.get('season',0),saved.get('episode',0),position=int(saved.get('position',0)*1000)))
            self.image(item.get('backdrop','') or item.get('poster',''),tile,276,154)
            l.addWidget(tile)
        l.addStretch();parent.addWidget(EdgeRail(w,175))

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
        self.sidebar.show()
        self.page_id+=1
        old=self.stack.currentWidget()
        if self.stack.indexOf(w)<0:self.stack.addWidget(w)
        self.stack.setCurrentWidget(w)
        if old and old is not w and old is not getattr(self,'player_page',None):
            if old is self._home_screen and self._home_ready:
                # Keep the fully-rendered Home widget alive so returning is instant.
                pass
            else:
                self.stack.removeWidget(old);old.deleteLater()
                if old is self._home_screen:
                    self._home_screen=None;self._home_ready=False
    def page(self,title):
        w,l=column();w.setObjectName('screenContent')
        l.setAlignment(Qt.AlignmentFlag.AlignTop)
        l.setContentsMargins(30,20,30,34);l.setSpacing(14)
        if title!='BruniO':
            heading=label(title,28);heading.setObjectName('pageTitle');l.addWidget(heading)
        scroll=QScrollArea();scroll.setWidgetResizable(True)
        scroll.setFrameShape(QScrollArea.Shape.NoFrame)
        scroll.setWidget(w);self.show_page(scroll);return l,self.page_id
    def image(self,url,w,width,height,contain=False):
        if not url:return
        # Deduplicate network requests and retain poster artwork across restarts.
        w.setProperty('_brunioImageUrl',url)
        def display(data,widget=w,target_w=width,target_h=height,fit=contain):
            if not isValid(widget) or widget.property('_brunioImageUrl')!=url:return
            pix=QPixmap()
            if not pix.loadFromData(data):return
            pix=pix.scaled(target_w,target_h,
                Qt.AspectRatioMode.KeepAspectRatio if fit
                else Qt.AspectRatioMode.KeepAspectRatioByExpanding,
                Qt.TransformationMode.SmoothTransformation)
            if hasattr(widget,'setPixmap'):widget.setPixmap(pix)
            else:
                widget.setIcon(QIcon(pix))
                if isinstance(widget,QPushButton):widget.setText('')
        if url in self.images:
            display(self.images[url]);return
        cached=None
        cache_path=None
        if self.image_cache_dir is not None:
            cache_path=self.image_cache_dir/(hashlib.sha256(url.encode('utf-8')).hexdigest()+'.img')
            try:
                if cache_path.is_file() and 0<cache_path.stat().st_size<8*1024*1024:
                    cached=cache_path.read_bytes()
            except OSError:pass
        if cached and QPixmap().loadFromData(cached):
            if len(self.images)>240:self.images.clear()
            self.images[url]=cached;display(cached);return
        if url in self.pending_images:
            self.pending_images[url].append(display);return
        self.pending_images[url]=[display]
        self.image_queue.append((url,cache_path))
        self.pump_image_queue()
    def pump_image_queue(self):
        # Prioritize visible first-row art over hundreds of offscreen thumbnails.
        while self.image_running<6 and self.image_queue:
            url,cache_path=self.image_queue.pop(0)
            self.image_running+=1
            request=QNetworkRequest(QUrl(url));request.setTransferTimeout(12000)
            reply=self.network.get(request)
            def finish(url=url,cache_path=cache_path,reply=reply):
                callbacks=self.pending_images.pop(url,[])
                try:
                    if reply.error()==reply.NetworkError.NoError:
                        data=bytes(reply.readAll())
                        if 0<len(data)<8*1024*1024 and QPixmap().loadFromData(data):
                            if len(self.images)>240:self.images.clear()
                            self.images[url]=data
                            if cache_path is not None:
                                try:
                                    cache_path.write_bytes(data)
                                except OSError:pass
                            for callback in callbacks:callback(data)
                finally:
                    reply.deleteLater()
                    self.image_running-=1
                    self.pump_image_queue()
            reply.finished.connect(finish)
    def prune_image_cache(self):
        folder=self.image_cache_dir
        if folder is None:return
        try:
            images=sorted(folder.glob('*.img'),key=lambda p:p.stat().st_mtime,reverse=True)
            size=0
            for index,path in enumerate(images):
                size+=path.stat().st_size
                if index>=650 or size>300*1024*1024:path.unlink(missing_ok=True)
        except OSError:pass
    def discard_home_cache(self):
        page=self._home_screen;self._home_screen=None;self._home_ready=False
        if page is not None and isValid(page):
            if self.stack.currentWidget() is not page:
                self.stack.removeWidget(page);page.deleteLater()
    def catalog_cached(self,path,kind=None,params=None):
        """Reuse recent TMDB sections across Home, Browse and streaming pages."""
        key=(self.client.session.get('userId',''),self.client.token,
             path,kind,json.dumps(params or {},sort_keys=True,default=str))
        now=time.monotonic()
        with self._catalog_lock:
            result=self._catalog_cache.get(key)
        if result and now-result[0]<360:return result[1]
        rows=self.client.catalog(path,kind,params)
        if rows:
            with self._catalog_lock:
                if len(self._catalog_cache)>60:self._catalog_cache.clear()
                self._catalog_cache[key]=(time.monotonic(),rows)
        return rows

    def load_genre_art(self,genres):
        """Select genuine backdrops by TMDB genre, cached for fourteen days."""
        needed=[(key,gid) for key,gid in genres if key not in self._genre_art]
        if not needed:return dict(self._genre_art)
        from concurrent.futures import ThreadPoolExecutor
        def choose(pair):
            key,genre=pair
            if isinstance(genre,int):
                try:items=self.catalog_cached('/discover/movie','movie',{'with_genres':genre})
                except Exception:return key,''
            else:
                try:items=self.catalog_cached(genre,'movie')
                except Exception:return key,''
            return key,next((m['backdrop'] for m in items
                             if m.get('backdrop')), '')
        with ThreadPoolExecutor(max_workers=4) as pool:
            for key,art in pool.map(choose,needed):
                if art:self._genre_art[key]=art
        if self.image_cache_dir is not None and self._genre_art:
            try:
                path=self.image_cache_dir/'genre-art.json'
                path.write_text(json.dumps({'created':time.time(),
                                            'images':self._genre_art}),encoding='utf-8')
            except OSError:pass
        return dict(self._genre_art)

    def avatar_path(self,value):
        names={'ai:astronaut':'brunio_avatar_astronaut.png','ai:fox':'brunio_avatar_fox.png','ai:robot':'brunio_avatar_robot.png','ai:dragon':'brunio_avatar_dragon.png','special:photo':'brunio_avatar_special.png','special:extra':'brunio_avatar_extra.png','special:extra3':'brunio_avatar_special3.jpg'}
        if value.startswith('special:extra') and value[13:].isdigit() and int(value[13:])>=4:names[value]=f'brunio_avatar_special{value[13:]}.png'
        return ASSETS/names.get(value,'brunio_avatar_astronaut.png')
    def card(self,item):
        w,l=column();l.setContentsMargins(0,0,0,0);l.setSpacing(7);w.setFixedWidth(162)
        pic=Poster();pic.setFixedSize(162,224);pic.setCursor(Qt.CursorShape.PointingHandCursor)
        pic.setToolTip(item['title']+f" · {item.get('year','')} · ★ {item.get('rating',0):.1f}");pic.setAccessibleName(item['title'])
        pic.clicked.connect(lambda checked=False:self.open(item));self.image(item.get('poster',''),pic,158,220);l.addWidget(pic)
        title=QLabel();title.setTextFormat(Qt.TextFormat.PlainText);title.setText(title.fontMetrics().elidedText(item['title'],Qt.TextElideMode.ElideRight,156));title.setToolTip(item['title']);title.setStyleSheet('font-size:13px;color:#ededff;font-weight:600;');l.addWidget(title)
        year=label(str(item.get('year','')),12);year.setStyleSheet('font-size:12px;color:#9b9fa6;');l.addWidget(year);return w
    def rail(self,parent,title,items):
        parent.addWidget(self.section_title(title));w,l=row();l.setSpacing(12);l.setContentsMargins(0,4,0,12)
        for item in items:l.addWidget(self.card(item))
        l.addStretch();parent.addWidget(EdgeRail(w,292))
    def load_profile(self):
        self.client.language='es-ES' if self.preferences.get('language')=='es' else 'en-US';self.sync_source_settings()
        def load():return self.client.state(),self.client.rest('miflix_ratings?'+urlencode({'profile_id':'eq.'+self.client.profile,'select':'cloud_id,score','limit':1000}))
        def done(value):
            self.refresh_profile_icon();self.state=value[0];self.ratings={x['cloud_id']:x['score'] for x in value[1]};self.persist();self.home()
        self.run(load,done)
    def home(self):
        if not self.client.session:self.settings();return
        self.select_nav('home')
        owner=self.client.session.get('userId','')+':'+self.client.profile
        if (self._home_screen is not None and isValid(self._home_screen)
                and self._home_ready and self._home_owner==owner
                and time.monotonic()-self._home_loaded_at<360):
            self.show_page(self._home_screen)
            return
        self.discard_home_cache()
        l,page=self.page('BruniO');l.setSpacing(14)
        self._home_screen=self.stack.currentWidget()
        self._home_owner=owner
        self._home_ready=False
        progress=dict(self.state.get('progress',{}))
        ids=[k for k,v in sorted(progress.items(),key=lambda x:x[1].get('updatedAt',0),reverse=True)
             if ':s' not in k and v.get('position',0)>=5 and v.get('percent',0)<90][:12]
        hero_slot=QWidget();hero_slot.setObjectName('heroSlot')
        hs=QVBoxLayout(hero_slot);hs.setContentsMargins(0,0,0,0);hs.setSpacing(0)
        hero_art=Art(hero=True);hero_art.setObjectName('cinematicHero')
        hero_art.setFixedHeight(418);hs.addWidget(hero_art)
        l.addWidget(hero_slot)
        platforms_slot=QWidget();platforms_layout=QVBoxLayout(platforms_slot)
        platforms_layout.setContentsMargins(0,0,0,0);platforms_layout.setSpacing(2)
        l.addWidget(platforms_slot)
        shelves=QWidget();shelves_layout=QVBoxLayout(shelves)
        shelves_layout.setContentsMargins(0,0,0,0);shelves_layout.setSpacing(2)
        l.addWidget(shelves)
        # Display bundled streaming artwork before waiting for any API requests.
        self.platform_rail(platforms_layout,page)
        def load():
            paths=[('Tendencias','Trending','/trending/all/week',None),
                   ('Películas populares','Popular movies','/movie/popular','movie'),
                   ('Series populares','Popular series','/tv/popular','series'),
                   ('Mejor valoradas','Top rated','/movie/top_rated','movie'),
                   ('En cartelera','Now playing','/movie/now_playing','movie'),
                   ('Series destacadas','Top series','/tv/top_rated','series')]
            import concurrent.futures
            def section(entry):
                try:return self.t(entry[0],entry[1]),self.catalog_cached(entry[2],entry[3])
                except Exception:return self.t(entry[0],entry[1]),[]
            def resume(key):
                try:return self.client.by_id(key)
                except Exception:return None
            with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
                rows=list(pool.map(section,paths))
                continued=[x for x in pool.map(resume,ids) if x]
            return rows,continued
        def done(value):
            if page!=self.page_id:return
            rows,continued=value
            features=rows[0][1][:5] if rows and rows[0][1] else []
            if features:
                featured={'index':0}
                area=QVBoxLayout(hero_art);area.setContentsMargins(30,26,30,26)
                area.setSpacing(9);area.addStretch(1)
                kicker=label(self.t('BRUNIO  /  SELECCIÓN DESTACADA','BRUNIO  /  FEATURED PICK'),12)
                kicker.setObjectName('heroKicker');area.addWidget(kicker)
                headline=label('',47);headline.setObjectName('heroHeadline');headline.setMaximumWidth(660)
                area.addWidget(headline)
                meta=label('',13);meta.setObjectName('heroMeta');area.addWidget(meta)
                story=label('',14);story.setObjectName('heroStory');story.setMaximumWidth(510)
                area.addWidget(story)
                actions,buttons=row();actions.setObjectName('heroActions');buttons.setSpacing(10)
                play=button(self.t('▶   Ver ahora','▶   Watch now'),lambda:self.start(features[featured['index']],1 if features[featured['index']]['type']=='series' else 0,1 if features[featured['index']]['type']=='series' else 0))
                play.setObjectName('primaryAction');buttons.addWidget(play)
                info=button(self.t('Más información','More info'),lambda:self.open(features[featured['index']]))
                buttons.addWidget(info);buttons.addStretch(1)
                dots=[]
                for i in range(len(features)):
                    dot=button('',lambda i=i:display(i));dot.setObjectName('heroDot')
                    dot.setFixedSize(22,6);buttons.addWidget(dot);dots.append(dot)
                next_hero=button('›',lambda:display((featured['index']+1)%len(features)))
                next_hero.setObjectName('heroArrow');next_hero.setFixedSize(38,38)
                buttons.addSpacing(12);buttons.addWidget(next_hero)
                area.addWidget(actions)
                def display(index):
                    if page!=self.page_id:return
                    featured['index']=index;item=features[index]
                    headline.setText(item.get('title',''))
                    meta.setText(f"{item.get('year','')}    ·    {self.t('Serie','Series') if item.get('type')=='series' else self.t('Película','Movie')}    ·    ★ {item.get('rating',0)}    ·    4K")
                    story.setText(item.get('overview','')[:190])
                    self.image(item.get('backdrop',''),hero_art,1600,530)
                    for i,dot in enumerate(dots):
                        dot.setProperty('selected',i==index)
                        dot.style().unpolish(dot);dot.style().polish(dot)
                display(0)
            else:
                empty=label(self.t('Tu próxima historia comienza aquí.','Your next story starts here.'),25)
                empty.setAlignment(Qt.AlignmentFlag.AlignCenter)
                hs.addWidget(empty)
            if continued:self.continue_rail(shelves_layout,continued)
            for name,items in rows:
                if items:self.rail(shelves_layout,name,items)
            self._home_ready=True
            self._home_loaded_at=time.monotonic()
            self.run(self.recommendations,lambda items:self.rail(shelves_layout,self.t('Para ti','For you'),items) if page==self.page_id and items else None,quiet=True)
        self.run(load,done)
    def recommendations(self):
        seeds=[k for k,v in sorted(self.ratings.items(),key=lambda x:x[1],reverse=True) if v>=6]+self.state.get('favorites',[])
        items={}
        for cloud in list(dict.fromkeys(seeds))[:3]:
            m=self.client.by_id(cloud);kind='tv' if m['type']=='series' else 'movie'
            for item in self.catalog_cached(f"/{kind}/{m['id']}/recommendations",m['type']):items[item['cloudId']]=item
        return list(items.values())
    def random_movie(self):
        if self.client.party and not self.client.host():self.tell(self.t('El host elige el contenido','The host chooses the title'));return
        def choose():
            rows=[x for x in self.recommendations() if x['type']=='movie'] or self.catalog_cached('/movie/top_rated','movie')
            rows=[x for x in rows if self.ratings.get(x['cloudId'],10)>4 and self.state.get('progress',{}).get(x['cloudId'],{}).get('percent',0)<80]
            if not rows:raise ValueError('No available movies')
            return random.choice(rows)
        self.run(choose,lambda item:self.start(item,0,0))
    def grid(self,l,items):
        grid=QGridLayout();grid.setSpacing(16)
        columns=max(1,min(8,(self.width()-265)//186))
        for i,item in enumerate(items):grid.addWidget(self.card(item),i//columns,i%columns)
        l.addLayout(grid)
    def search(self):
        self.select_nav('search');l,page=self.page('BruniO')
        l.setSpacing(17)
        bar,bl=row();bar.setObjectName('searchBar');bar.setMaximumWidth(980)
        bar.setMinimumHeight(62);bar.setMinimumWidth(460)
        bl.setContentsMargins(12,4,12,4);bl.setSpacing(8)
        glyph=QLabel();glyph.setPixmap(icon('search','#b9b3ec').pixmap(24,24));bl.addWidget(glyph)
        q=QLineEdit();q.setObjectName('searchInput')
        q.setPlaceholderText(self.t('Buscar películas, series, personas…','Search movies, shows, people…'))
        q.setClearButtonEnabled(True);bl.addWidget(q,1)
        glow=QGraphicsDropShadowEffect(bar);glow.setBlurRadius(23)
        glow.setColor(QColor(139,116,245,102));glow.setOffset(0,0)
        bar.setGraphicsEffect(glow)
        l.addSpacing(8);l.addWidget(bar,0,Qt.AlignmentFlag.AlignHCenter)
        filter_state={'value':'all','items':[],'people':[],'query':'','serial':0}
        filters=[('all','Todo','All'),('movie','Películas','Movies'),('series','Series','Series'),
                 ('actors','Actores','Actors'),('directors','Directores','Directors'),('genres','Géneros','Genres')]
        select_filter=None
        results_title=label(self.t('Descubre algo nuevo','Discover something new'),15)
        results_title.setObjectName('searchResultTitle')
        recent,rl=row();recent.setObjectName('recentSearches');rl.setSpacing(8)
        owner=self.client.session.get('userId','')+':'+self.client.profile
        for previous in self.history.get(owner,[])[:5]:
            b=button('⌕  '+previous,lambda text=previous:(q.setText(text),debounce.stop(),submit()))
            b.setObjectName('historyPill');rl.addWidget(b)
        rl.addStretch()
        if self.history.get(owner,[]):l.addWidget(recent)
        l.addWidget(results_title)
        results=QWidget();results.setObjectName('searchResults')
        result_l=QVBoxLayout(results);result_l.setAlignment(Qt.AlignmentFlag.AlignTop)
        result_l.setContentsMargins(0,0,0,0);result_l.setSpacing(14)
        l.addWidget(results)
        def clear_results():
            while result_l.count():
                child=result_l.takeAt(0)
                if child.widget():child.widget().deleteLater()
                if child.layout():
                    nested=child.layout()
                    while nested.count():
                        x=nested.takeAt(0)
                        if x.widget():x.widget().deleteLater()
        def render():
            if page!=self.page_id:return
            clear_results()
            kind=filter_state['value'];query=filter_state['query']
            if kind=='genres':
                results_title.setText(self.t('Explorar por género','Browse by genre'))
                grid=QGridLayout();grid.setSpacing(12)
                genres=[('Acción','Action',28),('Comedia','Comedy',35),('Drama','Drama',18),
                        ('Terror','Horror',27),('Ciencia ficción','Sci-Fi',878),
                        ('Suspenso','Thriller',53),('Animación','Animation',16),('Aventura','Adventure',12)]
                for i,(es,en,gid) in enumerate(genres):
                    b=button(self.t(es,en),lambda es=es,en=en,gid=gid:self.catalog_page(self.t(es,en),'/discover/movie','movie',{'with_genres':gid}))
                    b.setObjectName('genreTile');b.setMinimumHeight(78);grid.addWidget(b,i//4,i%4)
                result_l.addLayout(grid);return
            if kind in ('actors','directors'):
                people=[x for x in filter_state['people'] if (x.get('known_for_department') or 'Acting').lower()==('acting' if kind=='actors' else 'directing')]
                results_title.setText((self.t('Actores','Actors') if kind=='actors' else self.t('Directores','Directors'))+(f' · {query}' if query else ''))
                if people:
                    grid=QGridLayout();grid.setSpacing(14)
                    cols=max(2,min(7,(self.width()-265)//186))
                    for i,person in enumerate(people):grid.addWidget(self.person_card(person),i//cols,i%cols)
                    result_l.addLayout(grid)
                elif query:result_l.addWidget(label(self.t('No encontramos personas para esta búsqueda.','No people found for this search.'),14))
                return
            rows=[x for x in filter_state['items'] if kind=='all' or x.get('type')==kind]
            results_title.setText((self.t('Resultados para ','Results for ')+'“'+query+'”') if query else self.t('Tendencias para descubrir','Trending to discover'))
            if rows:self.grid(result_l,rows)
            else:result_l.addWidget(label(self.t('No se encontraron títulos. Prueba otra búsqueda.','No titles found. Try a different search.'),14))
        def on_filter(kind):
            filter_state['value']=kind
            if select_filter:select_filter(kind)
            if kind in ('actors','directors') and filter_state['query'] and not filter_state['people']:
                fetch_people(filter_state['query'])
            else:render()
        select_filter=self.filter_pills(l,filters,on_filter)
        # Move the filter control from the appended position to under the search field.
        filter_bar=l.itemAt(l.count()-1).widget()
        l.removeWidget(filter_bar);filter_bar.setMaximumWidth(880)
        l.insertWidget(2,filter_bar,0,Qt.AlignmentFlag.AlignHCenter)
        def fetch_people(query):
            serial=filter_state['serial']
            def found(data):
                if page!=self.page_id or serial!=filter_state['serial']:return
                filter_state['people']=data;render()
            self.run(lambda:self.client.tmdb('/search/person',{'query':query}).get('results',[])[:50],found,quiet=True)
        def submit():
            query=q.text().strip();filter_state['serial']+=1
            serial=filter_state['serial'];filter_state['query']=query
            filter_state['people']=[]
            if len(query)<2:
                if not query:
                    self.run(lambda:self.catalog_cached('/trending/all/week'),lambda rows:receive(rows,serial),quiet=True)
                return
            self.history[owner]=([query]+[old for old in self.history.get(owner,[]) if normalize(old)!=normalize(query)])[:10]
            self.persist();results_title.setText(self.t('Buscando…','Searching…'))
            def receive(rows,request=serial):
                if page!=self.page_id or request!=filter_state['serial']:return
                filter_state['items']=rows;render()
            self.run(lambda:self.client.search(query),receive)
            if filter_state['value'] in ('actors','directors'):fetch_people(query)
        def receive(rows,request):
            if page!=self.page_id or request!=filter_state['serial']:return
            filter_state['items']=rows;render()
        debounce=QTimer(results);debounce.setSingleShot(True);debounce.setInterval(420)
        debounce.timeout.connect(submit)
        q.textChanged.connect(lambda _:debounce.start())
        q.returnPressed.connect(lambda:(debounce.stop(),submit()))
        q.setFocus()
        self.run(lambda:self.catalog_cached('/trending/all/week'),lambda rows:receive(rows,filter_state['serial']),quiet=True)
    def person_card(self,person):
        w,l=column();l.setContentsMargins(0,0,0,0);l.setSpacing(6);w.setFixedWidth(160)
        tile=Poster();tile.setFixedSize(160,204);tile.setAccessibleName(str(person.get('name','')))
        tile.clicked.connect(lambda checked=False:self.person_detail(person))
        path=person.get('profile_path')
        if path:self.image('https://image.tmdb.org/t/p/w342'+path,tile,154,198)
        else:tile.setText(person.get('name',''))
        l.addWidget(tile)
        name=label(person.get('name',''),13);name.setObjectName('personName');l.addWidget(name)
        return w
    def person_detail(self,person):
        name=person.get('name',self.t('Persona','Person'))
        l,page=self.page(name)
        l.addWidget(label(self.t('Películas y series relacionadas','Related movies and shows'),15))
        def load():
            details=self.client.tmdb('/person/'+str(person['id'])+'/combined_credits')
            seen={}
            from .core import media as convert
            for row in details.get('cast',[])+details.get('crew',[]):
                if row.get('media_type') not in ('movie','tv') or not row.get('poster_path'):continue
                kind='series' if row['media_type']=='tv' else 'movie'
                item=convert(row,kind);seen[item['cloudId']]=item
            return sorted(seen.values(),key=lambda x:x.get('rating',0),reverse=True)[:60]
        self.run(load,lambda rows:self.grid(l,rows) if page==self.page_id else None)
    def genre_tile(self,name,gid,description=''):
        tile=ArtworkTile(name,description)
        tile.setObjectName('genreTile')
        tile.clicked.connect(lambda checked=False,genre=gid,label=name:
                             self.catalog_page(label,'/discover/movie','movie',
                                               {'with_genres':genre}))
        tile.setMinimumHeight(149)
        saved=self._genre_art.get('genre:'+str(gid),'')
        if saved:self.image(saved,tile,470,225)
        return tile
    def fill_genre_art(self,page,targets):
        # Map artwork to the exact genre rather than generic gradients.
        requests=[(key,gid) for key,gid,tile in targets]
        for key,gid,tile in targets:
            url=self._genre_art.get(key,'')
            if url and isValid(tile):self.image(url,tile,500,270)
        if all(key in self._genre_art for key,_,_ in targets):return
        def received(data):
            if page!=self.page_id:return
            for key,gid,tile in targets:
                url=data.get(key,'')
                if url and isValid(tile):self.image(url,tile,500,270)
        self.run(lambda:self.load_genre_art(requests),received,quiet=True)
    def collections(self):
        self.select_nav('collections')
        l,page=self.page('BruniO')
        header,hl=row();hl.setSpacing(10)
        headlines,headline_col=column()
        headline_col.setContentsMargins(0,0,0,0);headline_col.setSpacing(2)
        title=label(self.t('Explorar','Explore'),30);title.setObjectName('pageTitle')
        headline_col.addWidget(title)
        subtitle=label(self.t('Encuentra una nueva obsesión.','Find your next obsession.'),14)
        subtitle.setObjectName('muted');headline_col.addWidget(subtitle)
        hl.addWidget(headlines,1);l.addWidget(header)
        l.addWidget(self.section_title(self.t('Tus plataformas favoritas','Your favorite platforms')))
        services=[
            ('Netflix',8,'netflix.png','Series y películas','Movies and series'),
            ('Disney+',337,'disney-plus.png','Magia para todos','Discover magic'),
            ('Prime Video',9,'prime-video.png','Películas y originales','Films and originals'),
            ('Apple TV+',350,'apple-tv.png','Originales premiados','Award-winning originals'),
            ('HBO Max',1899,'hbo-max.png','Grandes producciones','Premium storytelling'),
            ('Paramount+',531,'paramount-plus.png','Cine y televisión','Films and shows'),
            ('Hulu',15,'hulu.png','Series que enamoran','Must-see series'),
            ('Crunchyroll',283,'crunchyroll.png','El mundo del anime','Anime universe'),
        ]
        provider_grid=QGridLayout();provider_grid.setSpacing(13)
        provider_grid.setContentsMargins(0,0,0,0)
        for i,(name,pid,image_name,es,en) in enumerate(services):
            tile=ArtworkTile(name,self.t(es,en))
            tile.setObjectName('collectionTile')
            tile.setFixedHeight(165)
            path=PLATFORM_ASSETS/image_name
            if path.is_file():tile.setPixmap(QPixmap(str(path)))
            tile.clicked.connect(lambda checked=False,name=name,pid=pid:
                                 self.provider(name,pid))
            tile.setSizePolicy(QSizePolicy.Policy.Expanding,QSizePolicy.Policy.Fixed)
            provider_grid.addWidget(tile,i//4,i%4)
        l.addLayout(provider_grid)
        l.addWidget(self.section_title(self.t('Descubre por género','Browse by genre'),
                         self.t('Colecciones visuales para cada momento.',
                                'Explore collections for every mood.')))
        genres=[('Acción','Action',28),('Comedia','Comedy',35),
                ('Drama','Drama',18),('Terror','Horror',27),
                ('Ciencia ficción','Sci-Fi',878),('Suspenso','Thriller',53),
                ('Animación','Animation',16),('Aventura','Adventure',12)]
        art_targets=[]
        genre_grid=QGridLayout();genre_grid.setSpacing(13)
        genre_grid.setContentsMargins(0,0,0,0)
        for i,(es,en,gid) in enumerate(genres):
            name=self.t(es,en)
            tile=self.genre_tile(name,gid,
                 self.t('Ver colección  ↗','Browse collection  ↗'))
            genre_grid.addWidget(tile,i//4,i%4)
            art_targets.append(('genre:'+str(gid),gid,tile))
        l.addLayout(genre_grid)
        l.addWidget(self.section_title(self.t('Colecciones destacadas','Featured collections')))
        special,sl=row();sl.setSpacing(13)
        for title_es,title_en,path,extra in [
            ('Próximamente','Coming soon','/movie/upcoming',
             self.t('Lo mejor está por llegar','See what is next')),
            ('Mejor valoradas','Top rated','/movie/top_rated',
             self.t('Lo más aclamado','Critically acclaimed'))]:
            tile=ArtworkTile(self.t(title_es,title_en),extra)
            tile.setObjectName('collectionWide');tile.setFixedHeight(152)
            tile.clicked.connect(lambda checked=False,title=self.t(title_es,title_en),
                         path=path:self.catalog_page(title,path,'movie'))
            sl.addWidget(tile,1)
            art_targets.append(('special:'+path,path,tile))
        l.addWidget(special)
        self.fill_genre_art(page,art_targets)
        l.addWidget(self.section_title(self.t('Viaja en el tiempo','Explore by year')))
        years,yl=row();yl.setSpacing(9)
        for year in range(time.localtime().tm_year,2015,-1):
            chip=button(str(year),lambda year=year:self.catalog_page(
                str(year),'/discover/movie','movie',{'primary_release_year':year}))
            chip.setObjectName('yearPill');chip.setFixedSize(92,40);yl.addWidget(chip)
        yl.addStretch();l.addWidget(EdgeRail(years,58))
        older=button(self.t('Explorar años anteriores  ↗','Explore older years  ↗'),
                    self.years_picker)
        older.setObjectName('secondaryAction');older.setMaximumWidth(260)
        l.addWidget(older)
    def years_picker(self):
        years=[str(y) for y in range(time.localtime().tm_year,1899,-1)]
        value,ok=QInputDialog.getItem(self,'BruniO',self.t('Selecciona el año','Choose a year'),years,0,False)
        if ok and value:self.catalog_page(value,'/discover/movie','movie',{'primary_release_year':int(value)})
    def catalog_page(self,title,path,kind,params=None):
        l,page=self.page(title);self.run(lambda:self.catalog_cached(path,kind,params),lambda items:self.grid(l,items) if page==self.page_id else None)
    def provider(self,title,pid):
        l,page=self.page(title)
        def load():
            base={'with_watch_providers':pid,'watch_region':'US'}
            rows=[(self.t('Películas','Movies'),self.catalog_cached('/discover/movie','movie',base)),(self.t('Series','Series'),self.catalog_cached('/discover/tv','series',base))]
            for genre,gid in [('Action',28),('Comedy',35),('Horror',27),('Drama',18),('Sci-Fi',878)]:rows.append((genre,self.catalog_cached('/discover/movie','movie',{**base,'with_genres':gid})))
            return rows
        self.run(load,lambda rows:[self.rail(l,name,items) for name,items in rows] if page==self.page_id else None)
    def my_list(self):
        self.select_nav('list');l,page=self.page(self.t('Mi Lista','My List'))
        ids=list(self.state.get('favorites',[]))
        state={'items':[],'active':'all'}
        result=QWidget();result_l=QVBoxLayout(result)
        result_l.setContentsMargins(0,0,0,0);result_l.setSpacing(12)
        result_l.setAlignment(Qt.AlignmentFlag.AlignTop)
        def show_filtered():
            if page!=self.page_id:return
            while result_l.count():
                part=result_l.takeAt(0)
                if part.widget():part.widget().deleteLater()
                if part.layout():
                    lay=part.layout()
                    while lay.count():
                        item=lay.takeAt(0)
                        if item.widget():item.widget().deleteLater()
            active=state['active'];items=state['items']
            if active in ('movie','series'):items=[x for x in items if x['type']==active]
            if active=='upcoming':items=[x for x in items if x.get('year','').isdigit() and int(x['year'])>time.localtime().tm_year]
            if items:self.grid(result_l,items)
            else:result_l.addWidget(label(self.t('Todavía no hay títulos en esta sección.','There are no titles in this section yet.'),15))
        def change(kind):state['active']=kind;select_filter(kind);show_filtered()
        filters=[('all','Todo','All'),('movie','Películas','Movies'),('series','Series','Series'),('upcoming','Próximamente','Coming soon')]
        select_filter=self.filter_pills(l,filters,change)
        caption=label(self.t('Tu colección personal, en un solo lugar.','Your personal collection, all in one place.'),13)
        caption.setObjectName('muted');l.addWidget(caption);l.addWidget(result)
        if not ids:show_filtered();return
        def load():
            rows=[]
            for key in ids:
                try:
                    item=self.client.by_id(key)
                    if item:rows.append(item)
                except Exception:continue
            return rows
        def show(items):
            if page!=self.page_id:return
            state['items']=items;show_filtered()
        self.run(load,show)

    def open(self,item):
        self.selected=item;l,page=self.page('BruniO');l.setSpacing(18)
        top,back_row=row();back_row.addWidget(button(self.t('‹  Volver','‹  Back'),self.home))
        back_row.addStretch(1);l.addWidget(top)
        hero=Art(hero=True);hero.setObjectName('detailHero')
        hero.setMinimumHeight(425);hero.setMaximumHeight(470)
        body=QVBoxLayout(hero);body.setContentsMargins(32,30,32,30);body.setSpacing(9)
        body.addStretch(1)
        kind=label('BRUNIO  /  '+('SERIE' if item.get('type')=='series' else self.t('PELÍCULA','MOVIE')),12)
        kind.setObjectName('heroKicker');body.addWidget(kind)
        title=label(item.get('title',''),48);title.setMaximumWidth(720)
        title.setObjectName('heroHeadline');body.addWidget(title)
        meta=label(f"{item.get('year','')}    ·    {self.t('Serie','Series') if item['type']=='series' else self.t('Película','Movie')}    ·    ★ {item.get('rating',0)}    ·    4K",13)
        meta.setObjectName('heroMeta');body.addWidget(meta)
        overview=label(item.get('overview','')[:300],14);overview.setMaximumWidth(600)
        overview.setObjectName('heroStory');body.addWidget(overview)
        actions,al=row();actions.setObjectName('heroActions');al.setSpacing(9)
        play=button(self.t('▶   Reproducir','▶   Play'),lambda:self.start(item,1 if item['type']=='series' else 0,1 if item['type']=='series' else 0))
        play.setObjectName('primaryAction');al.addWidget(play)
        al.addWidget(button(self.t('Fuentes','Sources'),lambda:self.start(item,1 if item['type']=='series' else 0,1 if item['type']=='series' else 0,manual=True)))
        al.addWidget(button('♡  '+self.t('Mi Lista','My List'),lambda:self.favorite(item)))
        al.addWidget(button('★',lambda:self.rate(item)))
        if item['type']=='movie':al.addWidget(button(self.t('Descargar','Download'),lambda:self.download_title(item)))
        al.addStretch(1);body.addWidget(actions)
        self.image(item.get('backdrop',''),hero,1500,560);l.addWidget(hero)
        saved=self.state.get('progress',{}).get(item['cloudId'],{})
        tabs,tl=row();tabs.setObjectName('detailTabs')
        tl.setSpacing(10)
        for name in (self.t('Información','Information'),self.t('Episodios','Episodes') if item['type']=='series' else self.t('Sinopsis','Synopsis')):
            tab=label(name,13);tab.setObjectName('detailTab');tl.addWidget(tab)
        tl.addStretch(1);l.addWidget(tabs)
        if saved.get('position',0):
            l.addWidget(button(self.t('▶  Continuar viendo','▶  Resume watching'),lambda:self.start(item,saved.get('season',0),saved.get('episode',0),position=int(saved['position']*1000))))
        if item['type']=='series':
            selector,sl=row()
            sl.addWidget(self.section_title(self.t('Episodios','Episodes')))
            sl.addStretch(1)
            combo=QComboBox();combo.setMinimumWidth(175);sl.addWidget(combo)
            sl.addWidget(button(self.t('Descargar temporada','Download season'),lambda:self.download_season(item,combo.currentData()) if combo.currentData() is not None else None))
            l.addWidget(selector)
            episodes=QWidget();el=QVBoxLayout(episodes);el.setContentsMargins(0,0,0,0)
            l.addWidget(episodes)
            def season_changed():
                season=combo.currentData()
                if season is None:return
                def show(rows):
                    if page!=self.page_id:return
                    while el.count():
                        x=el.takeAt(0)
                        if x.widget():x.widget().deleteLater()
                        if x.layout():
                            lay=x.layout()
                            while lay.count():
                                entry=lay.takeAt(0)
                                if entry.widget():entry.widget().deleteLater()
                    if not rows:
                        el.addWidget(label(self.t('No hay episodios disponibles.','No episodes available.'),13));return
                    shelf,layout=row();layout.setSpacing(12)
                    for ep in rows:
                        tile,vertical=column();tile.setFixedWidth(230)
                        tile.setObjectName('episodeCard');vertical.setContentsMargins(0,0,0,0);vertical.setSpacing(6)
                        thumb=Poster();thumb.setFixedSize(226,127)
                        thumb.clicked.connect(lambda checked=False,ep=ep:self.start(item,season,ep['episode_number']))
                        still=ep.get('still_path')
                        if still:self.image('https://image.tmdb.org/t/p/w500'+still,thumb,220,125)
                        else:thumb.setText('▶')
                        vertical.addWidget(thumb)
                        name=label(f"{ep['episode_number']}. {ep.get('name','')}",13)
                        name.setObjectName('episodeName');vertical.addWidget(name)
                        runtime=label(f"{ep.get('runtime') or '--'} min",11)
                        runtime.setObjectName('muted');vertical.addWidget(runtime)
                        layout.addWidget(tile)
                    layout.addStretch(1);el.addWidget(EdgeRail(shelf,185))
                self.run(lambda:self.client.episodes(item,season),show)
            combo.currentIndexChanged.connect(season_changed)
            self.run(lambda:self.client.details(item),lambda d:[combo.addItem(self.t('Temporada ','Season ')+str(n),n) for n in range(1,d.get('number_of_seasons',1)+1)] if page==self.page_id else None)
        else:
            l.addWidget(self.section_title(self.t('Sobre esta película','About this movie')))
            synopsis=label(item.get('overview',''),14);synopsis.setObjectName('synopsis');l.addWidget(synopsis)
        play.setFocus()
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
        self.player_page,l=column();self.player_page.setObjectName('player');l.setContentsMargins(22,14,22,18);l.setSpacing(8);top,tl=row();top.setObjectName('playerTop');self.title_label=label('',19);back=button('‹',self.stop_player);back.setObjectName('playerRound');back.setFixedSize(44,44);back.setToolTip(self.t('Volver','Back'));tl.addWidget(back);tl.addWidget(self.title_label,1);full=button('',self.fullscreen);full.setIcon(icon('fullscreen','#ffffff'));full.setToolTip(self.t('Pantalla completa','Fullscreen'));full.setObjectName('playerRound');full.setFixedSize(44,44);tl.addWidget(full);l.addWidget(top)
        self.video=QWidget();self.video.setAttribute(Qt.WidgetAttribute.WA_NativeWindow);self.video.setStyleSheet('background:black;');l.addWidget(self.video,1)
        self.pause_info=label('',16);self.pause_info.hide();l.addWidget(self.pause_info)
        controls_panel=QWidget();controls_panel.setObjectName('playerPanel');panel_layout=QVBoxLayout(controls_panel);panel_layout.setContentsMargins(18,10,18,12);panel_layout.setSpacing(8)
        self.seek=QSlider(Qt.Orientation.Horizontal);self.seek.setRange(0,1000);self.seek.sliderReleased.connect(self.seek_to);panel_layout.addWidget(self.seek)
        controls,cl=row();controls.setObjectName('playerControls');self.play_button=button('▶',self.toggle_play);self.play_button.setObjectName('playerPlay');self.play_button.setFixedSize(44,44);cl.addWidget(self.play_button)
        self.time_label=label('00:00 / 00:00');self.time_label.setStyleSheet('font-size:13px;color:#b6bac8;');cl.addWidget(self.time_label);volume_icon=button('',self.toggle_mute);volume_icon.setIcon(icon('volume','#ffffff'));volume_icon.setObjectName('playerRound');volume_icon.setFixedSize(40,40);volume_icon.setToolTip(self.t('Silenciar','Mute'));cl.addWidget(volume_icon)
        volume=QSlider(Qt.Orientation.Horizontal);volume.setFixedWidth(92);volume.setRange(0,100);volume.setValue(100);volume.valueChanged.connect(lambda value:self.player.audio_set_volume(value) if self.player else None);cl.addWidget(volume);cl.addStretch()
        cl.addWidget(button(self.t('Fuentes','Sources'),self.player_links))
        cl.addWidget(button(self.t('Audio / CC','Audio / CC'),self.subtitles))
        cl.addWidget(button(self.t('Llenar','Fill'),self.fit))
        cl.addWidget(button(self.t('Opciones','Options'),self.player_options))
        panel_layout.addWidget(controls);l.addWidget(controls_panel)
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
        self.play_id+=1;self.fallback_urls=set();self.sync_source_settings();ticket=self.play_id;self.tell(self.t('Buscando enlaces…','Finding playback links…'))
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
        self.current={'item':item,'season':season,'episode':episode,'stream':stream};self.fallback_urls.add(stream.get('url',''));self.source_activity=time.monotonic();self.source_last_pos=position;self.resume_pending=position;self.title_label.setText(item['title']+(f' · S{season} E{episode}' if season else ''));self.pause_info.hide();self.paused_at=0
        m=self.library.media_new(stream['url']);m.add_option(':audio-language='+self.profile_pref('audio',self.preferences.get('audio','es')));sub=self.profile_pref('sub',self.preferences.get('sub','es'));m.add_option(':sub-language='+('none' if sub=='off' else sub))
        hints=stream.get('behaviorHints',{}).get('proxyHeaders',{}).get('request',{})
        for header,opt in [('User-Agent','http-user-agent'),('Referer','http-referrer')]:
            if hints.get(header):m.add_option(':'+opt+'='+hints[header])
        if position:m.add_option(':start-time='+str(position/1000))
        self.player.set_media(m);self.sidebar.hide();self.stack.setCurrentWidget(self.player_page);self.player.play();m.release();self.tell(item['title'])
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
    def toggle_mute(self):
        if self.player:self.player.audio_toggle_mute()
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
        if event=='error':
            if not self.retry_next_source():self.tell(self.t('No se pudo reproducir. Prueba otro enlace.','Playback failed. Try another link.'))
        if event=='ended' and self.current:
            if self.client.party and not self.client.host():return
            c=dict(self.current)
            if c['item']['type']=='series' and self.profile_pref('autoplay',self.preferences.get('autoplay',True)):self.next_episode(c)
            else:self.open(c['item']);self.run(lambda:self.catalog_cached(f"/{'tv' if c['item']['type']=='series' else 'movie'}/{c['item']['id']}/recommendations",c['item']['type']),lambda rows:self.show_recommendations(rows))
    def retry_next_source(self):
        if not self.current or self.current['item']['type']=='live' or self.client.party or not self.profile_pref('retry_links',True):return False
        if len(self.fallback_urls)>=5:return False
        current=dict(self.current);candidate=next((x for x in self.sources if x.get('url') and x['url'] not in self.fallback_urls),None)
        if not candidate:return False
        position=max(0,self.source_last_pos,self.player.get_time()) if self.player else max(0,self.source_last_pos)
        self.tell(self.t('La fuente falló. Probando otro enlace…','Source failed. Trying another link…'))
        self.play_stream(current['item'],current['season'],current['episode'],position,candidate,self.imdb);return True
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
        self.sidebar.show()
        if self.isFullScreen():self.showNormal()
        item=self.current['item'] if self.current else None;self.current=None
        if item and item['type']!='live':self.open(item)
        else:self.home()
    def tick(self):
        if self.player and self.current:
            pos=max(0,self.player.get_time());duration=max(0,self.player.get_length());playing=bool(self.player.is_playing())
            if pos>self.source_last_pos:self.source_activity=time.monotonic();self.source_last_pos=pos
            if self.vlc and self.player.get_state() in (self.vlc.State.Opening,self.vlc.State.Buffering) and time.monotonic()-self.source_activity>30:
                self.source_activity=time.monotonic();self.retry_next_source()
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
        self.select_nav('friends');l,page=self.page(self.t('Amigos y Watch Party','Friends & Watch Party'))
        sub=label(self.t('Comparte lo que estás viendo.','Enjoy watching together.'),16);sub.setObjectName('muted');l.addWidget(sub)
        actions,al=row();actions.setMaximumWidth(1000)
        for text,fn in [('▶  Watch Party',self.party_controls),(self.t('+  Añadir amigo','+  Add friend'),self.add_friend),(self.t('Perfil social','Social profile'),self.social_settings),(self.t('Actualizar','Refresh'),self.friends)]:al.addWidget(button(text,fn))
        l.addWidget(actions);l.addWidget(label(self.t('Tu comunidad','Your community'),21))
        content=QWidget();content.setMaximumWidth(1050);box=QVBoxLayout(content);box.setContentsMargins(0,0,0,0);l.addWidget(content)
        def show(value):
            if page!=self.page_id:return
            activity,pending=value
            if not activity and not pending:box.addWidget(label(self.t('Añade amigos para ver su actividad aquí.','Add friends to see their activity here.'),15))
            for person in activity or []:
                card,rl=row();card.setObjectName('friendCard');rl.setContentsMargins(18,12,18,12)
                badge=label('●',20);badge.setStyleSheet('color:'+('#51da9c' if person.get('online') else '#667080')+';');rl.addWidget(badge)
                details,dl=column();dl.setContentsMargins(0,0,0,0);dl.setSpacing(2);dl.addWidget(label('@'+person.get('nickname',''),16));detail=label(person.get('title') or (self.t('En línea','Online') if person.get('online') else 'Offline'),13);detail.setObjectName('muted');dl.addWidget(detail);rl.addWidget(details,1)
                if person.get('room_code'):rl.addWidget(button(self.t('Unirse','Join'),lambda p=person:self.run(lambda:self.client.party_join(p['room_code']),self.joined,guard=False)))
                box.addWidget(card)
            for f in pending or []:
                if f['receiver_id']==self.client.social_identity() and f['status']=='pending':box.addWidget(button(self.t('Aceptar solicitud · ','Accept request · ')+f['sender_id'][:8],lambda f=f:self.run(lambda:self.client.social_rest('rp1_miflix_friendships?'+urlencode({'sender_id':'eq.'+f['sender_id'],'receiver_id':'eq.'+self.client.social_identity()}),'PATCH',{'status':'accepted'}),lambda _:self.friends())))
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
    def profiles(self,manage=False):
        # Full-width cinematic chooser.  No sidebar on profiles, just as on TV.
        view=Starfield();view.setObjectName('profileChooser')
        l=QVBoxLayout(view);l.setContentsMargins(32,26,32,45)
        scroller=QScrollArea();scroller.setWidgetResizable(True)
        scroller.setFrameShape(QScrollArea.Shape.NoFrame);scroller.setWidget(view)
        self.show_page(scroller);self.sidebar.hide()
        l.setAlignment(Qt.AlignmentFlag.AlignHCenter|Qt.AlignmentFlag.AlignTop)
        header,hl=row();hl.setSpacing(8)
        mark=label('BruniO',24);mark.setObjectName('profileBrand');hl.addWidget(mark)
        hl.addStretch(1)
        manage_button=button(self.t('Listo','Done') if manage else self.t('Administrar perfiles','Manage profiles'),lambda:self.profiles(not manage))
        manage_button.setObjectName('manageProfiles');hl.addWidget(manage_button)
        l.addWidget(header);l.addStretch(1)
        title=label(self.t('¿Quién está viendo?','Who is watching?'),29)
        title.setObjectName('profileQuestion');title.setAlignment(Qt.AlignmentFlag.AlignCenter)
        l.addWidget(title);l.addSpacing(20)
        panel,grid=row();grid.setAlignment(Qt.AlignmentFlag.AlignHCenter);grid.setSpacing(26)
        for profile in self.client.account.get('profiles',[]):
            card,box=column();card.setObjectName('profileChoice');card.setFixedWidth(155)
            box.setContentsMargins(0,0,0,0);box.setSpacing(10)
            pic=ProfileAvatar();pic.setFixedSize(146,146);pic.setToolTip(profile.get('name',''))
            value=profile.get('avatarValue') or 'ai:astronaut'
            if value.startswith('https://'):self.image(value,pic,146,146)
            elif value!='none':pic.setPixmap(QPixmap(str(self.avatar_path(value))))
            pic.clicked.connect(lambda checked=False,p=profile:self.avatar_picker(p) if manage else self.select_profile(p))
            box.addWidget(pic,0,Qt.AlignmentFlag.AlignHCenter)
            name=button(profile['name'],lambda p=profile:self.avatar_picker(p) if manage else self.select_profile(p))
            name.setObjectName('profileName');box.addWidget(name)
            if manage:
                actions,al=row();al.addWidget(button('Avatar',lambda p=profile:self.avatar_picker(p)))
                if len(self.client.account['profiles'])>1:
                    remove=button('✕',lambda p=profile:self.delete_profile(p))
                    remove.setToolTip(self.t('Eliminar perfil','Delete profile'))
                    remove.setFixedWidth(42);al.addWidget(remove)
                box.addWidget(actions)
            grid.addWidget(card)
        l.addWidget(panel,0,Qt.AlignmentFlag.AlignHCenter)
        add=button('＋  '+self.t('Agregar perfil','Add profile'),self.create_profile)
        add.setObjectName('addProfile');add.setFixedWidth(190)
        l.addSpacing(22);l.addWidget(add,0,Qt.AlignmentFlag.AlignHCenter)
        l.addStretch(2)
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
        from .playlist import read_m3u
        self.select_nav('live');l,page=self.page('BruniO')
        top,top_row=row()
        title=label('Live TV',30);title.setObjectName('pageTitle');top_row.addWidget(title)
        top_row.addStretch(1)
        live_tag=label('●  '+self.t('EN DIRECTO','LIVE NOW'),12)
        live_tag.setObjectName('liveBadge');top_row.addWidget(live_tag);l.addWidget(top)
        description=label(self.t('Descubre canales, deportes y programas en vivo.','Explore live TV, sports and entertainment.'),14)
        description.setObjectName('muted');l.addWidget(description)
        owner=self.client.session.get('userId','local')+':'+self.client.profile
        toolbar,bar=row();provider=QComboBox();provider.addItem('BruniO TV','tv');provider.addItem('IPTV · M3U','m3u');provider.addItem('Live sports','sports');provider.setMinimumWidth(145);bar.addWidget(provider)
        category=QComboBox();category.setMinimumWidth(145);bar.addWidget(category);search=QLineEdit();search.setPlaceholderText(self.t('Buscar canal…','Search channels…'));bar.addWidget(search,1)
        bar.addWidget(button(self.t('Importar M3U','Import M3U'),lambda:import_playlist()));bar.addWidget(button('Stats',self.sports_scores));l.addWidget(toolbar)
        def quick_select(key):
            if key in ('all','favorites'):
                index=category.findData('*' if key=='all' else 'favorites')
                if index>=0:category.setCurrentIndex(index)
                return
            synonyms={
                'sports':['deport','sport','fútbol','football','nba','mlb'],
                'movies':['cine','movie','film','película'],
                'series':['serie','show','24/7'],
                'news':['noticia','news','informativo'],
                'kids':['infantil','kids','niñ','animación','cartoon'],
            }
            targets=synonyms.get(key,[])
            for i in range(category.count()):
                group=normalize(str(category.itemData(i) or category.itemText(i)))
                if any(k in group for k in targets):
                    category.setCurrentIndex(i);return
            self.tell(self.t('Esa categoría no está disponible en esta fuente.','That category is unavailable for this source.'))
        filters=[('all','Todos','All'),('favorites','Favoritos','Favorites'),
                 ('sports','Deportes','Sports'),('movies','Cine','Movies'),
                 ('series','Series','Series'),('news','Noticias','News'),('kids','Infantil','Kids')]
        select_quick=self.filter_pills(l,filters,quick_select)
        quick_bar=l.itemAt(l.count()-1).widget()
        l.removeWidget(quick_bar)
        l.addWidget(EdgeRail(quick_bar,54))
        hint=label('',13);hint.setObjectName('channelCount');l.addWidget(hint)
        holder=QWidget();holder.setObjectName('liveChannels')
        hl=QGridLayout(holder);hl.setContentsMargins(0,0,0,0);hl.setSpacing(13);l.addWidget(holder)
        more=button(self.t('＋  Cargar más canales','＋  Load more channels'),lambda:show_more());more.setObjectName('secondaryAction');more.setMaximumWidth(240);more.hide();l.addWidget(more)
        state={'rows':[],'visible':56,'provider':'tv','manifest':''}
        def import_playlist():
            path,_=QFileDialog.getOpenFileName(self,self.t('Abrir lista M3U','Open M3U playlist'),'','M3U (*.m3u *.m3u8);;All files (*)')
            if not path:return
            self.preferences.setdefault('playlists',{})[owner]=path;self.persist();index=provider.findData('m3u')
            if provider.currentIndex()==index:categories()
            else:provider.setCurrentIndex(index)
        def show_more():state['visible']+=56;render()
        def render():
            if page!=self.page_id:return
            while hl.count():
                child=hl.takeAt(0)
                if child.widget():child.widget().deleteLater()
            cat=category.currentData();q=normalize(search.text());favorites=self.preferences.setdefault('live_favorites',{}).setdefault(owner,[])
            rows=[x for x in state['rows'] if (cat in (None,'*') or (cat=='favorites' and x['id'] in favorites) or x.get('category')==cat) and (not q or q in normalize(x.get('name','')))]
            self.live_candidates=[(channel,state['provider'],state['manifest']) for channel in rows]
            hint.setText(self.t(f'{len(rows):,} canales disponibles','{n:,} available channels'.format(n=len(rows)))+'   ·   '+str(provider.currentText()))
            columns=max(3,min(6,(self.content.width()-75)//205))
            for i,channel in enumerate(rows[:state['visible']]):
                card=QWidget();card.setObjectName('channelTile');card.setMinimumWidth(0);card.setSizePolicy(QSizePolicy.Policy.Ignored,QSizePolicy.Policy.Fixed);card.setFixedHeight(177)
                line=QVBoxLayout(card);line.setContentsMargins(15,12,15,13);line.setSpacing(7)
                head,hlayout=row();category_label=label('●  '+channel.get('category','LIVE'),10);category_label.setObjectName('channelCategory');category_label.setMaximumHeight(20);hlayout.addWidget(category_label,1)
                ident=str(channel.get('id',''));star=button('♥' if ident in favorites else '♡',lambda id=ident:toggle(id));star.setObjectName('favoritePill');star.setProperty('selected',ident in favorites);star.setFixedSize(32,32);hlayout.addWidget(star);line.addWidget(head)
                pic=QLabel(channel.get('name','TV')[:2].upper());pic.setObjectName('channelLogo');pic.setFixedSize(78,78);pic.setAlignment(Qt.AlignmentFlag.AlignCenter);line.addWidget(pic,0,Qt.AlignmentFlag.AlignHCenter)
                if channel.get('logo','').startswith(('https://','http://')):self.image(channel['logo'],pic,75,75,contain=True)
                name=button(channel.get('name','Channel'),lambda c=channel:play(c));name.setText(name.fontMetrics().elidedText(channel.get('name','Channel'),Qt.TextElideMode.ElideRight,130));name.setObjectName('channelName');name.setToolTip(channel.get('name',''));name.setMinimumWidth(0);name.setSizePolicy(QSizePolicy.Policy.Ignored,QSizePolicy.Policy.Fixed);line.addWidget(name)
                hl.addWidget(card,i//columns,i%columns)
            more.setVisible(len(rows)>state['visible'])
        def toggle(id):
            favorites=self.preferences.setdefault('live_favorites',{}).setdefault(owner,[])
            if id in favorites:favorites.remove(id)
            else:favorites.append(id)
            self.persist();render()
        def play(c):
            if state['provider'] in ('tv','m3u'):
                if self.client.party:self.tell(self.t('Los canales locales no se comparten en Party.','Local channels cannot be shared in Party.'));return
                item={'id':0,'type':'live','cloudId':'tmdb:live:'+str(java_hash(c['id'])),'title':c.get('name','Channel'),'overview':'','liveChannelId':c['id'],'liveProvider':'custom','liveType':'tv'}
                self.play_id+=1;self.play_stream(item,0,0,0,{'url':c['url'],'name':c['name'],'behaviorHints':{'proxyHeaders':{'request':c.get('headers',{})}}})
            else:self.play_channel(c,'sports',state['manifest'])
        def apply(rows,key,manifest=''):
            if page!=self.page_id or provider.currentData()!=key:return
            state.update(rows=rows,provider=key,manifest=manifest,visible=56);category.blockSignals(True);category.clear();category.addItem(self.t('Todos','All'),'*');category.addItem(self.t('Favoritos','Favorites'),'favorites')
            for group in sorted(set(x.get('category','Others') for x in rows),key=str.casefold):category.addItem(group,group)
            category.blockSignals(False);render()
        def categories():
            key=provider.currentData();state['rows']=[];state['visible']=56;render()
            if key in ('tv','m3u'):
                path=ASSETS/'final_channels_rp97.m3u' if key=='tv' else self.preferences.get('playlists',{}).get(owner,'')
                if not path:hint.setText(self.t('Importa una lista M3U para comenzar.','Import an M3U playlist to begin.'));return
                self.run(lambda:read_m3u(path),lambda rows:apply(rows,key));return
            manifest=self.live_manifest('sports')
            def load():
                body=self.client.transport(manifest);result=[]
                for cat in body.get('catalogs',[]):
                    url=manifest.removesuffix('/manifest.json')+f"/catalog/{quote(cat.get('type','tv'))}/{quote(cat['id'])}.json"
                    try:
                        result.extend({**entry,'category':cat.get('name',cat['id'])} for entry in (self.client.transport(url).get('metas',[])))
                    except Exception:continue
                return result
            self.run(load,lambda rows:apply(rows,key,manifest))
        provider.currentIndexChanged.connect(categories);category.currentIndexChanged.connect(lambda:(state.update(visible=56),render()));search.textChanged.connect(lambda:(state.update(visible=56),render()));categories()
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
            del notices[100:];self.alert_button.setText(self.t('Notificaciones','Notifications')+('  ·  '+str(len(notices)) if notices else ''));self.persist()
            if done:done(notices)
        self.run(load,received,quiet=True)
    def alerts(self):
        l,page=self.page(self.t('Notificaciones','Notifications'));owner=self.client.session.get('userId','')+':'+self.client.profile
        sub=label(self.t('Novedades de tus favoritos.','Updates from your favorites.'),15);sub.setObjectName('muted');l.addWidget(sub)
        def clear():self.preferences.setdefault('notifications',{})[owner]=[];self.alert_button.setText(self.t('Notificaciones','Notifications'));self.persist();shown([])
        clear_button=button(self.t('Borrar notificaciones','Clear notifications'),clear);clear_button.setMaximumWidth(210);l.addWidget(clear_button)
        holder=QWidget();holder.setMaximumWidth(900);box=QVBoxLayout(holder);box.setContentsMargins(0,0,0,0);l.addWidget(holder)
        def shown(rows):
            if page!=self.page_id:return
            while box.count():
                child=box.takeAt(0)
                if child.widget():child.widget().deleteLater()
            if not rows:
                card=label(self.t('No hay notificaciones por ahora.','No notifications right now.'),16);card.setObjectName('noticeCard');box.addWidget(card)
            for text in rows:
                card=label(text,16);card.setObjectName('noticeCard');box.addWidget(card)
        shown(self.preferences.get('notifications',{}).get(owner,[]));self.refresh_notices(shown)
    def settings(self,refresh=True):
        self.select_nav('settings')
        if refresh and self.client.session:
            l,page=self.page('BruniO')
            l.addWidget(label(self.t('Sincronizando configuración…','Syncing settings…'),16))
            self.run(self.client.load_account,
                     lambda _:(self.refresh_profile_icon(),self.settings(refresh=False))
                     if page==self.page_id else None)
            return

        outer,page=self.page('BruniO')
        title=label(self.t('Ajustes','Settings'),30)
        title.setObjectName('pageTitle');outer.addWidget(title)
        subtitle=label(self.t('Tu espacio BruniO, hecho a tu medida.',
                             'Your BruniO, just the way you like it.'),14)
        subtitle.setObjectName('muted');outer.addWidget(subtitle)

        shell,shell_row=row();shell.setObjectName('settingsShell')
        shell_row.setSpacing(24);shell_row.setAlignment(Qt.AlignmentFlag.AlignTop)
        sidebar=QWidget();sidebar.setObjectName('settingsNav')
        sidebar.setFixedWidth(215)
        nav=QVBoxLayout(sidebar);nav.setSpacing(7)
        nav.setContentsMargins(12,17,12,20)
        content=QWidget();content.setObjectName('settingsContent')
        body=QVBoxLayout(content);body.setAlignment(Qt.AlignmentFlag.AlignTop)
        body.setSpacing(0);body.setContentsMargins(0,0,0,0)
        shell_row.addWidget(sidebar);shell_row.addWidget(content,1)
        outer.addWidget(shell)

        panels={}
        def pane(key,heading,detail):
            w=QWidget();w.setObjectName('settingsPane')
            v=QVBoxLayout(w);v.setAlignment(Qt.AlignmentFlag.AlignTop)
            v.setContentsMargins(0,0,0,20);v.setSpacing(17)
            heading_label=label(heading,24)
            heading_label.setObjectName('settingsPageHeading')
            v.addWidget(heading_label)
            secondary=label(detail,13);secondary.setObjectName('muted');v.addWidget(secondary)
            body.addWidget(w);w.hide()
            panels[key]=(w,v)
            return v
        def card(parent,title,description=''):
            w=QWidget();w.setObjectName('settingsCard')
            v=QVBoxLayout(w);v.setContentsMargins(24,23,24,24);v.setSpacing(13)
            heading=label(title,18);heading.setObjectName('settingsGroupHeading')
            v.addWidget(heading)
            if description:
                desc=label(description,12);desc.setObjectName('muted');v.addWidget(desc)
            parent.addWidget(w)
            return v
        account=pane('account',self.t('Mi cuenta','My account'),
                     self.t('Perfiles y seguridad de tu cuenta.','Profiles and account security.'))
        playback=pane('playback',self.t('Reproducción','Playback'),
                      self.t('Personaliza tus películas y episodios.','Tune your movies and episodes.'))
        addons=pane('addons',self.t('Complementos','Add-ons'),
                    self.t('Fuentes conectadas y prioridad.','Connected sources and priority.'))
        appearance=pane('appearance',self.t('Apariencia','Appearance'),
                        self.t('Elige el estilo perfecto para tu pantalla.','Find the best look for your screen.'))
        updates=pane('updates',self.t('Actualizaciones','Updates'),
                     self.t('Mantén BruniO PC siempre al día.','Keep BruniO PC up to date.'))
        advanced=pane('advanced',self.t('Más opciones','More options'),
                      self.t('Herramientas y almacenamiento.','Tools and storage.'))
        nav_buttons={}
        selected={'key':getattr(self,'_settings_section','account')}
        entries=[
            ('account','profiles','Cuenta','Account'),
            ('playback','movies','Reproducción','Playback'),
            ('addons','collections','Complementos','Add-ons'),
            ('appearance','settings','Apariencia','Appearance'),
            ('updates','downloads','Actualizaciones','Updates'),
            ('advanced','list','Almacenamiento','Storage'),
        ]
        def show_section(key):
            if key not in panels:key='account'
            selected['key']=key;self._settings_section=key
            for name,(widget,_) in panels.items():
                widget.setVisible(name==key)
            for name,control in nav_buttons.items():
                control.setProperty('selected',name==key)
                control.style().unpolish(control);control.style().polish(control)
            content.adjustSize();shell.adjustSize()
        for key,glyph,es,en in entries:
            b=button(self.t(es,en),lambda key=key:show_section(key))
            b.setObjectName('settingsNavButton')
            b.setIcon(icon(glyph,'#aea5e3'));b.setIconSize(QSize(18,18))
            b.setFixedHeight(47);nav.addWidget(b)
            nav_buttons[key]=b
        nav.addStretch(1)

        update_card=card(updates,self.t('BruniO para Windows','BruniO for Windows'),
                         self.t('Las versiones se descargan desde tu repositorio oficial de GitHub.',
                                'Updates come directly from your official GitHub repository.'))
        version_label=label(self.t('Versión instalada: ','Installed version: ')+VERSION,15)
        version_label.setObjectName('versionBadge');update_card.addWidget(version_label)
        updater=button(self.t('↻   Buscar actualización','↻   Check for updates'),
                       self.check_updates)
        updater.setObjectName('updateButton');updater.setMaximumWidth(265)
        update_card.addWidget(updater)
        update_card.addWidget(label(self.t(
            'La descarga comprueba la versión, el tamaño y SHA-256 antes de abrir el instalador.',
            'Installer version, size and SHA-256 are verified before launch.'),12))

        looks=card(appearance,self.t('Tema de la aplicación','Application theme'),
                   self.t('El modo automático sigue la apariencia de Windows.',
                          'Automatic follows your Windows appearance.'))
        theme_row,trl=row();trl.setSpacing(12)
        for key,es,en in [('dark','🌙  Oscuro','🌙  Dark'),
                          ('light','☀  Claro','☀  Light'),
                          ('auto','◐  Automático','◐  Auto')]:
            choice=button(self.t(es,en),lambda key=key:self.change_theme(key))
            choice.setObjectName('themeOption');choice.setMinimumHeight(54)
            choice.setProperty('selected',self.preferences.get('theme','dark')==key)
            trl.addWidget(choice,1)
        looks.addWidget(theme_row)
        looks.addWidget(label(self.t('Los cambios se aplican al instante.',
                                      'Changes apply instantly.'),12))
        note=card(appearance,self.t('Experiencia visual','Visual experience'))
        note.addWidget(label(self.t(
            'Contraste mejorado, navegación tipo glass y carátulas cinematográficas.',
            'Enhanced contrast, glass navigation, and cinematic artwork.'),13))

        if not self.client.session:
            login_area=card(account,self.t('Inicia sesión','Sign in'))
            email=QLineEdit();email.setPlaceholderText('Email')
            password=QLineEdit();password.setPlaceholderText('Password')
            password.setEchoMode(QLineEdit.EchoMode.Password)
            login_area.addWidget(email);login_area.addWidget(password)
            def sign_in():
                e=email.text().strip();p=password.text();password.clear()
                self.run(lambda:self.client.login(e,p),
                         lambda _:(self.refresh_profile_icon(),self.profiles()),
                         guard=False)
            sign_in_button=button(self.t('Iniciar sesión','Sign in'),sign_in)
            sign_in_button.setObjectName('primaryAction');login_area.addWidget(sign_in_button)
            login_area.addWidget(button(self.t('Acceder con QR','Sign in using QR'),self.qr_login))
            password.returnPressed.connect(sign_in)
            for empty,es,en in [('playback','Inicia sesión para configurar la reproducción.',
                                       'Sign in to configure playback.'),
                                ('addons','Inicia sesión para administrar tus fuentes.',
                                       'Sign in to manage your sources.')]:
                card(panels[empty][1],self.t(es,en))
        else:
            info=card(account,self.t('Cuenta conectada','Connected account'))
            info.addWidget(label(self.client.session.get('email',''),16))
            change_profile=button(self.t('Cambiar perfil  ↗','Switch profile  ↗'),self.profiles)
            change_profile.setObjectName('secondaryAction');change_profile.setMaximumWidth(230)
            info.addWidget(change_profile)
            controls=card(account,self.t('Tu cuenta','Your account'))
            controls.addWidget(button('Trakt',self.trakt))
            logout=button(self.t('Cerrar sesión','Sign out'),self.sign_out)
            logout.setObjectName('secondaryAction');controls.addWidget(logout)

            general=card(playback,self.t('Idioma y subtítulos','Language and subtitles'))
            for es,en,key,choices in [
                ('Idioma de la app','App language','language',['es','en']),
                ('Audio preferido','Preferred audio','audio',['es','en','pt','fr','de','ja']),
                ('Subtítulos','Subtitles','sub',['es','en','pt','fr','de','off'])
            ]:
                item,il=row();il.addWidget(label(self.t(es,en)),1)
                combo=QComboBox();combo.setMinimumWidth(175)
                combo.addItems(choices)
                current=(self.preferences.get(key,choices[0]) if key=='language'
                         else self.profile_pref(key,self.preferences.get(key,choices[0])))
                combo.setCurrentText(current)
                combo.currentTextChanged.connect(
                    lambda value,key=key:self.change_language(value)
                    if key=='language' else self.save_profile_pref(key,value))
                il.addWidget(combo);general.addWidget(item)
            options=card(playback,self.t('Comportamiento del reproductor','Player behavior'))
            retry=QCheckBox(self.t('Reintentar otro enlace si falla',
                                    'Try another source when playback fails'))
            retry.setChecked(self.profile_pref('retry_links',True))
            retry.toggled.connect(lambda value:self.save_profile_pref('retry_links',value))
            options.addWidget(retry)
            autoplay=QCheckBox(self.t('Reproducir el siguiente episodio automáticamente',
                                       'Automatically play the next episode'))
            autoplay.setChecked(self.profile_pref('autoplay',
                                                   self.preferences.get('autoplay',True)))
            autoplay.toggled.connect(lambda value:self.save_profile_pref('autoplay',value))
            options.addWidget(autoplay)
            limit_row,lr=row();lr.addWidget(label(self.t('Límite de tamaño',
                                                        'Source size limit')),1)
            limit=QDoubleSpinBox();limit.setRange(0,500);limit.setSuffix(' GB')
            limit.setValue(self.preferences.get('limit',0))
            limit.setSpecialValueText(self.t('Sin límite','Unlimited'))
            limit.valueChanged.connect(lambda value:self.save_pref('limit',value))
            lr.addWidget(limit);options.addWidget(limit_row)

            sources=card(addons,self.t('Catálogo y credenciales','Catalog and credentials'))
            token=QLineEdit(self.client.token);token.setEchoMode(QLineEdit.EchoMode.Password)
            token.setPlaceholderText('TMDB token');sources.addWidget(token)
            addon=QLineEdit();addon.setPlaceholderText('https://…/manifest.json')
            addon.setEchoMode(QLineEdit.EchoMode.Password);sources.addWidget(addon)
            def save_setup():
                url=addon.text().strip()
                if url and not valid_url(url):self.tell('Invalid manifest URL');return
                if url and not url.endswith('/manifest.json'):
                    url=url.rstrip('/')+'/manifest.json'
                token_value=token.text().strip()
                self.run(lambda:self.client.edit_setup(token=token_value,add=url or None),
                         lambda _:(self.tell(self.t('Configuración guardada',
                                                       'Settings saved')),self.settings()))
            save=button(self.t('Guardar configuración','Save configuration'),save_setup)
            save.setObjectName('primaryAction');save.setMaximumWidth(245)
            sources.addWidget(save)
            connected=card(addons,self.t('Tus complementos','Your add-ons')+
                           '  ·  '+str(len(self.client.manifests)))
            for manifest in self.client.manifests:
                host=__import__('urllib.parse',fromlist=['urlsplit']).urlsplit(manifest).hostname or ''
                item,il=row()
                text=label(host,14);il.addWidget(text,1)
                remove=button(self.t('Eliminar','Remove'),
                              lambda m=manifest:self.run(
                                  lambda:self.client.edit_setup(remove=m),
                                  lambda _:self.settings()))
                remove.setObjectName('secondaryAction');remove.setFixedWidth(104)
                il.addWidget(remove);connected.addWidget(item)
            priority=button(self.t('Prioridad de complementos  ↗','Add-on priority  ↗'),
                            self.addon_priority)
            priority.setObjectName('secondaryAction');connected.addWidget(priority)
            sports_area=card(addons,self.t('Deportes en vivo','Live sports'))
            sports=QLineEdit(self.preferences.get('sports_manifest',''))
            sports.setPlaceholderText('Live sports manifest URL (optional)')
            sports.setEchoMode(QLineEdit.EchoMode.Password);sports_area.addWidget(sports)
            def save_sports():
                url=sports.text().strip()
                if url and not valid_url(url):self.tell('Invalid manifest URL');return
                self.save_pref('sports_manifest',url)
                self.tell(self.t('Deportes guardados','Sports settings saved'))
            sports_area.addWidget(button(self.t('Guardar deportes','Save sports'),save_sports))

        storage=card(advanced,self.t('Almacenamiento local','Local storage'))
        storage.addWidget(label(self.t(
            'Libera miniaturas y recursos descargados temporalmente sin perder tus listas.',
            'Clear temporary thumbnails without losing your lists.'),13))
        clear=button(self.t('Limpiar caché de imágenes','Clear image cache'),
                     lambda:(self.images.clear(),
                             self.tell(self.t('Caché limpiado','Image cache cleared'))))
        clear.setObjectName('secondaryAction');storage.addWidget(clear)
        storage.addWidget(button(self.t('Ver descargas  ↗','View downloads  ↗'),self.downloads))
        show_section(selected['key'])

    def change_theme(self,choice):
        if choice not in ('dark','light','auto'):return
        self.preferences['theme']=choice;self.persist()
        self.apply_theme()
        self._settings_section='appearance'
        self.settings(refresh=False)

    def apply_theme(self):
        app=QApplication.instance()
        if not app:return
        choice=self.preferences.get('theme','dark')
        system_dark=app.styleHints().colorScheme()==Qt.ColorScheme.Dark
        light=(choice=='light') or (choice=='auto' and not system_dark)
        app.setProperty('lightTheme',light)
        app.setStyleSheet(STYLE+CINEMATIC_STYLE+(LIGHT_STYLE if light else ''))

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
                        self.run(lambda:self.client.edit_setup(add=addon),lambda _:(self.refresh_profile_icon(),self.profiles()))
                    else:self.refresh_profile_icon();self.profiles()
                d.accept();self.run(self.client.load_account,loaded,guard=False)
            else:info.setText('Use the Account section on your phone to sign in')
            self.run(pairing.finish,quiet=True)
        def poll():
            if busy[0]:return
            busy[0]=True;self.run(pairing.poll,received,finally_=lambda:busy.__setitem__(0,False))
        timer.timeout.connect(poll);self.run(pairing.create,ready);d.exec();active[0]=False;timer.stop();self.run(pairing.finish,quiet=True)

    def profile_scope(self):return self.client.session.get('userId','local')+':'+self.client.profile
    def profile_pref(self,key,default=None):return self.preferences.get('profile_playback',{}).get(self.profile_scope(),{}).get(key,default)
    def save_profile_pref(self,key,value):
        self.preferences.setdefault('profile_playback',{}).setdefault(self.profile_scope(),{})[key]=value;self.sync_source_settings();self.persist()
    def sync_source_settings(self):
        self.client.source_audio=self.profile_pref('audio',self.preferences.get('audio','es'));language='es' if self.client.source_audio=='es' else 'en'
        self.client.source_order=self.profile_pref('addon_order_'+language,[])
    def addon_priority(self):
        from .core import ordered_addons,addon_id
        d=QDialog(self);d.setWindowTitle(self.t('Prioridad de complementos','Add-on priority'));d.resize(560,480);layout=QVBoxLayout(d)
        layout.addWidget(label(self.t('El orden se guarda para cada perfil y audio.','Order is saved per profile and audio.'),14))
        language=QComboBox();language.addItem(self.t('Español','Spanish'),'es');language.addItem(self.t('Inglés','English'),'en');layout.addWidget(language)
        items=QListWidget();layout.addWidget(items,1);state={'rows':[]}
        def show():
            current=language.currentData();state['rows']=ordered_addons(self.client.manifests,current,self.profile_pref('addon_order_'+current,[]));items.clear()
            from urllib.parse import urlsplit
            for row in state['rows']:items.addItem(urlsplit(row).hostname or 'Add-on')
        def move(offset):
            index=items.currentRow();target=index+offset
            if index<0 or target<0 or target>=len(state['rows']):return
            rows=state['rows'];rows[index],rows[target]=rows[target],rows[index];self.save_profile_pref('addon_order_'+language.currentData(),[addon_id(x) for x in rows]);show();items.setCurrentRow(target)
        bar,bl=row();bl.addWidget(button('↑  '+self.t('Subir','Move up'),lambda:move(-1)));bl.addWidget(button('↓  '+self.t('Bajar','Move down'),lambda:move(1)));bl.addWidget(button(self.t('Restablecer','Reset'),lambda:(self.save_profile_pref('addon_order_'+language.currentData(),[]),show())));layout.addWidget(bar)
        language.currentIndexChanged.connect(show);show();d.exec()
    def change_language(self,value):
        self.save_pref('language',value)
        for key,(es,en) in self.nav_labels.items():self.nav_buttons[key].setText(self.t(es,en))
        self.nav_buttons['settings'].setText(self.t('Ajustes','Settings'))
        self.alert_button.setText(self.t('Notificaciones','Notifications'))
        self.more_button.setText(self.t('Más','More')+('  ⌃' if not self.extra_nav.isHidden() else '  ⌄'))
        self.settings(refresh=False)

    def save_pref(self,key,value):self.preferences[key]=value;self.client.language='es-ES' if self.preferences.get('language')=='es' else 'en-US';self.sync_source_settings();self.persist()
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
            if not info:self.tell(self.t('Ya tienes la última versión de BruniO PC.','You have the latest PC version.'));return
            if QMessageBox.question(self,'BruniO',self.t('¿Descargar la versión ','Download version ')+info['version']+'?')!=QMessageBox.StandardButton.Yes:return
            def downloaded(path):
                if QMessageBox.question(self,'BruniO',self.t('Instalador verificado. ¿Cerrar BruniO e instalar?','Installer verified. Close BruniO and install?'))==QMessageBox.StandardButton.Yes:
                    os.startfile(str(path));self.close()
            self.tell(self.t('Descargando actualización…','Downloading update…'));self.run(lambda:download(info),downloaded)
        self.tell(self.t('Buscando actualización de BruniO PC…','Checking for BruniO PC updates…'));self.run(check,done)
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
QWidget { color:#ecedff; font-family:"Segoe UI"; font-size:13px; }
QMainWindow,QWidget#appRoot,QWidget#contentShell,QWidget#screenContent { background:#060c17; }
QWidget#sidebar { background:qlineargradient(x1:0,y1:0,x2:1,y2:1,stop:0 #060d19,stop:1 #0c1222); border-right:1px solid #273146; }
QWidget#sidebar QLabel { background:transparent; border:0; }
QLabel#brand { color:#eeebff; font-size:26px; font-weight:800; letter-spacing:-1px; padding-left:9px; }
QPushButton { color:#e1e2f3; background:rgba(28,35,52,200); border:1px solid #3d4560; border-radius:19px; padding:10px 18px; font-weight:600; }
QPushButton:hover { color:#ffffff; background:rgba(62,67,99,225); border-color:#8981b9; }
QPushButton:pressed { background:#696090; }
QPushButton:focus { border:1px solid #c5b7ff; }
QPushButton#pillButton { background:#1c2536; border:1px solid #45506a; border-radius:18px; }
QPushButton#navButton { text-align:left; background:transparent; border:1px solid transparent; border-radius:10px; padding:7px 10px; font-size:12px; font-weight:500; color:#b6b8ce; }
QPushButton#navButton:hover { color:#f3f0ff; background:#242c42; border:1px solid #2c3853; }
QPushButton#navButton[selected="true"] { background:qlineargradient(x1:0,y1:0,x2:1,y2:1,stop:0 #4b477c,stop:1 #292f4e); color:#ffffff; border:1px solid #5e5888; font-weight:700; }
QPushButton#roundButton,QPushButton#profile { background:#141c2b; border:1px solid #34415a; border-radius:20px; padding:5px; }
QPushButton#roundButton:hover,QPushButton#profile:hover { border-color:#bcaeff; background:#293554; }
QLabel#profileFootnote { color:#9ca2bd; font-size:11px; }
QLabel#pageTitle { font-size:28px; color:#f1efff; font-weight:700; padding-bottom:3px; }
QWidget#sectionHeader,QWidget#flatRow,QWidget#edgeRail,QWidget#heroActions,QWidget#searchResults,QWidget#filterBar,QWidget#recentSearches,QWidget#detailTabs { background:transparent; border:0; }
QLabel#sectionTitle { color:#edeaff; font-size:18px; font-weight:700; }
QLabel#heroKicker { background:transparent; color:#c1aeff; letter-spacing:2px; font-size:11px; font-weight:800; }
QLabel#heroHeadline { background:transparent; color:#ffffff; font-family:"Georgia"; font-size:47px; font-weight:600; }
QLabel#heroMeta { background:transparent; color:#d3d8ee; font-size:12px; font-weight:600; }
QLabel#heroStory { background:transparent; color:#e2e5f2; font-size:14px; }
QPushButton#primaryAction { color:#15132c; background:qlineargradient(x1:0,y1:0,x2:1,y2:0,stop:0 #aa93ff,stop:.5 #c4b8ff,stop:1 #a998fa); border:1px solid #d3caff; border-radius:20px; padding:11px 23px; font-weight:800; }
QPushButton#primaryAction:hover { background:#e0d6ff; color:#16112b; }
QPushButton#heroDot { border:0; background:transparent; color:#505b7b; font-size:15px; padding:0; border-radius:7px; }
QPushButton#heroDot[selected="true"] { color:#c4b5ff; }
QPushButton#platformTile,QPushButton#continueCard { background:transparent; border:0; padding:0; }
QPushButton#railEdge { color:white; background:rgba(20,24,40,195); border:1px solid #6b618c; border-radius:16px; padding:4px; }
QPushButton#railEdge:hover { background:#494271; }
QPushButton#filterPill { color:#c2c4d6; background:rgba(19,25,38,175); border:1px solid #43506a; border-radius:16px; padding:7px 16px; font-weight:500; }
QPushButton#filterPill:hover { border-color:#a49bd5; }
QPushButton#filterPill[selected="true"] { color:#17152d; background:#b5a7fd; border-color:#c6bbff; font-weight:800; }
QPushButton#historyPill { background:#151e2d; color:#aab1cc; border:1px solid #29384d; font-size:11px; padding:8px 13px; }
QWidget#searchBar { background:#171d31; border:1px solid #8478c2; border-radius:26px; }
QLineEdit#searchInput { background:transparent; border:0; padding:6px 10px; font-size:17px; color:white; }
QLineEdit#searchInput:focus { border:0; }
QLabel#searchResultTitle { font-size:14px; font-weight:700; color:#dcdcf5; }
QPushButton#genreTile { border-radius:18px; font-size:16px; background:qlineargradient(x1:0,y1:0,x2:1,y2:1,stop:0 #252945,stop:1 #111a2c); }
QLabel#personName { color:#dedcf0; font-size:13px; }
QLabel#detailTab { color:#c5baff; font-weight:700; border-bottom:2px solid #9185cb; padding:9px 11px; }
QWidget#episodeCard { background:transparent; }
QLabel#episodeName { font-size:12px; font-weight:600; }
QLabel#synopsis { color:#bfc4da; font-size:13px; }
QLabel#muted { color:#989fb9; }
QWidget#profileChoice { background:transparent; }
QLabel#profileQuestion { background:transparent; font-size:29px; color:white; font-weight:800; }
QLabel#profileBrand { background:transparent; font-size:24px; font-weight:800; color:#c4b5ff; }
QPushButton#profileName { background:transparent; border:0; color:#eae8f8; font-size:14px; padding:7px 2px; }
QPushButton#manageProfiles { background:#141b2d; border:1px solid #565376; color:#d9d5f8; font-size:12px; }
QPushButton#addProfile { background:#211c38; border:1px solid #a495de; color:#e3daff; }
QWidget#profileCard,QWidget#friendCard,QWidget#settingsCard,QWidget#channelTile,QWidget#downloadCard,QWidget#updateStrip { background:#151e2d; border:1px solid #2b3950; border-radius:17px; }
QWidget#profileCard QLabel,QWidget#friendCard QLabel,QWidget#settingsCard QLabel,QWidget#channelTile QLabel,QWidget#downloadCard QLabel,QWidget#updateStrip QLabel { background:transparent; border:0; }
QWidget#channelTile:hover { background:#253149; border-color:#766cad; }
QLabel#channelCategory { color:#a1a8c3; font-size:10px; }
QPushButton#favoritePill { background:#252740; border:1px solid #514e77; border-radius:16px; padding:0; }
QPushButton#channelName { background:transparent; border:0; padding:6px 2px; }
QPushButton#updateButton { color:#201b36; background:#b9aafc; border:0; border-radius:20px; }
QWidget#player { background:#03050c; }
QWidget#playerPanel { background:#151d2c; border:1px solid #3c4169; border-radius:22px; }
QWidget#playerControls,QWidget#playerTop { background:transparent; }
QPushButton#playerPlay { background:#baabff; color:#101326; border:0; border-radius:22px; font-size:17px; padding:0; }
QLabel#status { padding:9px 28px; color:#f5f1ff; background:#373157; }
QLabel#noticeCard { background:#151d2d; border:1px solid #343f5b; border-radius:18px; padding:22px; }
QWidget#multiTile { background:#0b1020; border:1px solid #474364; border-radius:14px; }
QLineEdit,QComboBox,QDoubleSpinBox { padding:10px 14px; border:1px solid #48516e; border-radius:18px; background:#161d2d; selection-background-color:#8b78d5; }
QLineEdit:focus,QComboBox:focus { border:1px solid #b9a8ff; }
QScrollArea { border:0; background:#060c17; }
QSlider::groove:horizontal { height:4px; background:#48516d; border-radius:2px; }
QSlider::sub-page:horizontal { background:#af9eff; border-radius:2px; }
QSlider::handle:horizontal { width:14px; margin:-5px 0; background:#c9bcff; border-radius:7px; }
QScrollBar:vertical { width:7px; background:transparent; }
QScrollBar::handle:vertical,QScrollBar::handle:horizontal { background:#484963; border-radius:3px; min-height:24px; }
QScrollBar::add-line,QScrollBar::sub-line { height:0; width:0; }
QScrollBar:horizontal { height:7px; background:transparent; }
QCalendarWidget QWidget { alternate-background-color:#222538; }
QCalendarWidget QToolButton { background:#2b2c49; color:#ecebff; border:0; border-radius:12px; padding:6px; }
QCalendarWidget QAbstractItemView { background:#151a2c; color:#f0ecff; selection-background-color:#686198; selection-color:white; }
QDialog { background:#0e1525; }
'''



LIGHT_STYLE='''
QWidget { color:#18223a; }
QMainWindow,QWidget#appRoot,QWidget#contentShell,QWidget#screenContent,QScrollArea { background:#f1f3fa; }
QWidget#sidebar { background:#e9edf7; border-right:1px solid #d5dcec; }
QLabel#brand,QLabel#pageTitle,QLabel#sectionTitle,QLabel#settingsPageHeading,
QLabel#settingsGroupHeading { color:#202a45; }
QWidget#settingsContent,QWidget#settingsPane,QWidget#searchResults { background:transparent; }
QWidget#settingsCard,QWidget#updateStrip,QWidget#channelTile,QWidget#downloadCard {
    background:#ffffff; border:1px solid #d6dfed; }
QWidget#settingsNav { background:#ffffff; border:1px solid #d6dfed; }
QWidget#profileFooter { background:#ffffff; border:1px solid #d6dfed; }
QLabel#profileDisplayName,QLabel#profileFootnote { color:#26314a; }
QLabel#muted,QLabel#channelCategory,QLabel#channelCount { color:#65718d; }
QPushButton { color:#26314a; background:#ffffff; border:1px solid #ced7e9; }
QPushButton:hover { color:#171c31; background:#edf0fb; border:1px solid #a79ae0; }
QPushButton#navButton,QPushButton#settingsNavButton { color:#45516b; background:transparent; }
QPushButton#navButton:hover,QPushButton#settingsNavButton:hover { background:#e1e5f1; color:#1e2840; }
QPushButton#navButton[selected="true"],QPushButton#settingsNavButton[selected="true"] {
    color:#342659; background:#ddd3ff; border-color:#c2b4f4; }
QPushButton#collectionTile,QPushButton#genreTile,QPushButton#collectionWide { color:#f8f6ff; }
QPushButton#filterPill { color:#46516b; background:#ffffff; border:1px solid #cdd6e7; }
QPushButton#filterPill[selected="true"] { color:#251b46; background:#c9bcff; }
QWidget#searchBar,QLineEdit,QComboBox,QDoubleSpinBox {
    color:#1c2944; background:#ffffff; border:1px solid #becae4; }
QLineEdit#searchInput { color:#15203b; background:transparent; border:0; }
QPushButton#themeOption { background:#ffffff; color:#25314c; }
QPushButton#themeOption[selected="true"] { background:#d6caff; border-color:#9c87ef; color:#241943; }
QLabel#searchResultTitle { color:#26314a; }
QWidget#player { background:#03050c; }
'''
CINEMATIC_STYLE='''
QPushButton#navButton { border-radius:12px; padding:9px 13px; font-size:15px; font-weight:600; }
QPushButton#navButton:hover { background:#212c47; border-color:#414467; }
QPushButton#navButton[selected="true"] {
    background:qlineargradient(x1:0,y1:0,x2:1,y2:1,stop:0 #5f528f,stop:1 #302a50);
    border:1px solid #8376b2; font-weight:750; }
QWidget#profileFooter { background:#131c2d; border:1px solid #323951; border-radius:15px; }
QPushButton#profileAvatarButton {
    background:#1c2440; border:2px solid #a695ee; border-radius:23px; padding:2px; }
QPushButton#profileAvatarButton:hover { border-color:#e0d6ff; background:#414061; }
QLabel#profileDisplayName { font-size:13px; font-weight:700; color:#f3f0ff; }
QLabel#profileFootnote { font-size:11px; color:#a5a8c5; }
QPushButton#heroDot { border:0; background:#555f79; border-radius:3px; padding:0; }
QPushButton#heroDot[selected="true"] { background:#d0bbff; }
QPushButton#heroArrow {
    border:1px solid #726c97; background:rgba(15,20,37,177);
    border-radius:19px; color:#f2eaff; padding:0; font-size:28px; font-weight:400; }
QPushButton#heroArrow:hover { background:rgba(86,73,137,220); border-color:#cbb8ff; }
QPushButton#railEdge {
    background:rgba(27,29,50,215); border:1px solid #8276ad;
    color:#fff; border-radius:19px; padding:0; }
QPushButton#railEdge:hover { background:#71639b; border-color:#c7b2ff; }
QWidget#settingsNav { background:#101827; border:1px solid #303a52; border-radius:20px; }
QWidget#settingsContent,QWidget#settingsPane { background:transparent; }
QWidget#settingsCard { border-radius:20px; border:1px solid #333f59; background:#151e2d; }
QLabel#settingsPageHeading { color:#f3f0ff; font-weight:800; font-size:24px; }
QLabel#settingsGroupHeading { color:#efebff; font-weight:750; font-size:18px; }
QPushButton#settingsNavButton {
    text-align:left; border-radius:12px; padding:10px 14px; border:1px solid transparent;
    background:transparent; color:#b7bcd5; font-weight:600; }
QPushButton#settingsNavButton:hover { background:#273048; border-color:#454666; }
QPushButton#settingsNavButton[selected="true"] {
    background:#4f437b; border-color:#8370b2; color:#ffffff; }
QPushButton#themeOption { color:#e2dafa; background:#202940; border:1px solid #464c6a;
    border-radius:15px; font-size:14px; font-weight:700; }
QPushButton#themeOption:hover { background:#323052; border-color:#9485c7; }
QPushButton#themeOption[selected="true"] {
    color:#1a1434; background:#c7b6ff; border:1px solid #dbceff; }
QPushButton#updateButton { color:#1c1636; background:#c5b5ff; border:1px solid #d8ccff; border-radius:19px; font-weight:800; }
QPushButton#updateButton:hover { background:#e4dcff; }
QLabel#versionBadge { color:#c4b6ff; background:#21233c; padding:9px 14px;
    border:1px solid #514773; border-radius:11px; }
QPushButton#collectionTile {
    text-align:left; border-radius:18px; padding:18px 22px;
    background:qlineargradient(x1:0,y1:0,x2:1,y2:1,stop:0 #282743,stop:.55 #151d33,stop:1 #11192b);
    border:1px solid #444262; font-size:17px; line-height:1.4; }
QPushButton#collectionTile:hover { background:#3c315c; border:1px solid #a591ef; }
QPushButton#genreTile {
    text-align:left; border-radius:16px; padding:17px 20px;
    background:qlineargradient(x1:0,y1:0,x2:1,y2:1,stop:0 #292640,stop:1 #111c30);
    border:1px solid #454266; font-size:14px; }
QPushButton#genreTile:hover { background:#3a315a; border-color:#a995e9; }
QPushButton#collectionWide {
    text-align:left; padding:18px 25px; border-radius:16px;
    background:#1b2540; border:1px solid #434567; font-size:17px; }
QPushButton#collectionWide:hover { background:#343458; border-color:#a58fea; }
QPushButton#yearPill,QPushButton#secondaryAction {
    background:#1d2639; border:1px solid #4b526c; border-radius:18px;
    color:#d9ddf4; font-weight:600; }
QPushButton#yearPill:hover,QPushButton#secondaryAction:hover {
    background:#41385d; border-color:#ab98e6; }
QLabel#liveBadge { color:#ff7c8f; background:#262038; border:1px solid #5e3a55;
    border-radius:12px; padding:9px 13px; font-weight:800; }
QLabel#channelCount { color:#afb9d9; font-size:13px; }
QWidget#channelTile { background:#121b2b; border:1px solid #303d57; border-radius:18px; }
QWidget#channelTile:hover { background:#1e2940; border-color:#9580d7; }
QLabel#channelCategory { color:#a59cdb; font-size:11px; }
QPushButton#channelName { color:#f2f1ff; font-weight:650; padding:6px 2px;
    background:transparent; border:0; text-align:center; }
QPushButton#favoritePill { border:1px solid #625681; border-radius:16px;
    background:#262338; color:#f1e1fa; padding:0; font-size:16px; }
QPushButton#favoritePill[selected="true"] { background:#6e3c73; color:white; }
QWidget#searchBar { border-radius:29px; border:1px solid #aa97ee; background:#181d31; }
QLineEdit#searchInput { font-size:18px; }
QPushButton#filterPill { padding:8px 16px; font-size:12px; }
QPushButton#filterPill[selected="true"] { background:#c1b0ff; border-color:#d5c9ff; }
QPushButton#platformTile { border:0; background:transparent; padding:0; }
QPushButton#artworkTile { border:0; background:transparent; padding:0; }
'''
def main(smoke=False,smoke_vlc=False):
    app=QApplication(sys.argv);app.setStyleSheet(STYLE);window=Window(smoke);window.show()
    try:app.styleHints().colorSchemeChanged.connect(lambda _:window.apply_theme())
    except AttributeError:pass
    if smoke_vlc:window.ensure_player()
    if smoke:QTimer.singleShot(200,app.quit)
    return app.exec()
