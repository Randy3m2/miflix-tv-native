"""Native Qt visual components; no web view or extra runtime required."""
from PySide6.QtCore import Qt, QSize, QRectF, QByteArray, QPointF
from PySide6.QtGui import QPixmap, QIcon, QPainter, QPainterPath, QColor, QLinearGradient, QRadialGradient, QPolygonF, QPen
from PySide6.QtSvg import QSvgRenderer
from PySide6.QtWidgets import QWidget, QPushButton

PATHS={
 'home':'<path d="M3 11 12 3l9 8v10h-6v-7H9v7H3Z"/>',
 'search':'<circle cx="10" cy="10" r="7"/><path d="m15 15 6 6"/>',
 'collections':'<rect x="3" y="6" width="18" height="15" rx="3"/><path d="M7 3h10M3 11h18"/>',
 'friends':'<circle cx="9" cy="8" r="4"/><path d="M2 21v-3a7 7 0 0 1 14 0v3M17 5a4 4 0 0 1 0 8M19 16a5 5 0 0 1 3 5"/>',
 'live':'<rect x="2" y="6" width="20" height="15" rx="3"/><path d="m8 2 4 4 4-4m-6 14 5-3-5-3Z"/>',
 'movies':'<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M9 7h6m-6 4h6m-6 4h4"/>',
 'series':'<rect x="2" y="4" width="20" height="16" rx="3"/><path d="M6 23h12m-6-3v3"/>',
 'list':'<path d="M5 3h14v19l-7-5-7 5Z"/>',
 'calendar':'<rect x="3" y="5" width="18" height="17" rx="2"/><path d="M7 2v6M17 2v6M3 10h18"/>',
 'downloads':'<path d="M12 3v12m-5-5 5 5 5-5M3 17v4h18v-4"/>',
 'profiles':'<circle cx="12" cy="8" r="4"/><path d="M3 22v-3a9 9 0 0 1 18 0v3"/>',
 'settings':'<path d="m9 2-1 4-4 1-2 5 3 3v5l5 2 3-3 5 1 3-5-2-4 1-5-5-2-3 3Z"/><circle cx="12" cy="12" r="3"/>',
 'bell':'<path d="M4 18h16l-2-4V9a6 6 0 0 0-12 0v5ZM9 21h6"/>',
 'volume':'<path d="M3 10v4h4l5 4V6l-5 4H3Zm12-1a5 5 0 0 1 0 6m3-9a9 9 0 0 1 0 12"/>',
 'fullscreen':'<path d="M4 9V4h5m6 0h5v5M4 15v5h5m6 0h5v-5"/>',
 'right':'<path d="m9 4 8 8-8 8"/>',
 'left':'<path d="m15 4-8 8 8 8"/>',
}
def icon(name,color='#b6bac1'):
    svg=f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><g fill="none" stroke="{color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">{PATHS[name]}</g></svg>'
    pix=QPixmap(32,32);pix.fill(Qt.GlobalColor.transparent);p=QPainter(pix)
    QSvgRenderer(QByteArray(svg.encode())).render(p);p.end();return QIcon(pix)

class Art(QWidget):
    def __init__(self,parent=None,hero=False):
        super().__init__(parent);self.pix=QPixmap();self.hero=hero
        self.setAttribute(Qt.WidgetAttribute.WA_StyledBackground,False)
    def setPixmap(self,pix):self.pix=pix;self.update()
    def paintEvent(self,event):
        p=QPainter(self);p.setRenderHint(QPainter.RenderHint.Antialiasing)
        path=QPainterPath();path.addRoundedRect(QRectF(self.rect()),19,19);p.setClipPath(path);p.fillRect(self.rect(),QColor('#111724'))
        if not self.pix.isNull():
            scaled=self.pix.scaled(self.size(),Qt.AspectRatioMode.KeepAspectRatioByExpanding,Qt.TransformationMode.SmoothTransformation)
            p.drawPixmap((self.width()-scaled.width())//2,(self.height()-scaled.height())//2,scaled)
        if self.hero:
            gradient=QLinearGradient(0,0,self.width(),0);gradient.setColorAt(0,QColor(4,9,19,251));gradient.setColorAt(.37,QColor(5,11,22,227));gradient.setColorAt(.72,QColor(5,11,20,83));gradient.setColorAt(1,QColor(4,10,17,12));p.fillRect(self.rect(),gradient)
            shade=QLinearGradient(0,self.height()*.45,0,self.height());shade.setColorAt(0,QColor(4,8,16,0));shade.setColorAt(1,QColor(4,8,16,195));p.fillRect(self.rect(),shade)
        p.end()

class Poster(QPushButton):
    def __init__(self,parent=None):super().__init__(parent);self.pix=QPixmap();self.setObjectName('poster')
    def setPixmap(self,pix):self.pix=pix;self.update()
    def paintEvent(self,event):
        p=QPainter(self);p.setRenderHint(QPainter.RenderHint.Antialiasing)
        rect=QRectF(self.rect()).adjusted(3,3,-3,-3);path=QPainterPath();path.addRoundedRect(rect,11,11);p.setClipPath(path);p.fillRect(rect,QColor('#141b29'))
        if not self.pix.isNull():
            pix=self.pix.scaled(self.size()-QSize(6,6),Qt.AspectRatioMode.KeepAspectRatioByExpanding,Qt.TransformationMode.SmoothTransformation)
            p.drawPixmap((self.width()-pix.width())//2,(self.height()-pix.height())//2,pix)
        elif self.text():
            p.setPen(QColor('#f1edf8'));font=p.font();font.setBold(True);font.setPixelSize(19);p.setFont(font);p.drawText(rect,Qt.AlignmentFlag.AlignCenter,self.text())
        p.setClipping(False)
        if self.underMouse() or self.hasFocus():p.setPen(QPen(QColor('#bcaaff'),2));p.drawRoundedRect(QRectF(self.rect()).adjusted(1,1,-2,-2),12,12)
        p.end()
    def enterEvent(self,e):self.update();super().enterEvent(e)
    def leaveEvent(self,e):self.update();super().leaveEvent(e)

class EdgeRail(QWidget):
    """A horizontal shelf with navigation over its left and right edges."""
    def __init__(self,content,height,parent=None):
        from PySide6.QtWidgets import QScrollArea,QHBoxLayout
        super().__init__(parent);self.setObjectName('edgeRail');self.setFixedHeight(height)
        outer=QHBoxLayout(self);outer.setContentsMargins(0,0,0,0)
        self.scroll=QScrollArea();self.scroll.setWidgetResizable(True);self.scroll.setFrameShape(QScrollArea.Shape.NoFrame)
        self.scroll.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);self.scroll.setVerticalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);self.scroll.setWidget(content);outer.addWidget(self.scroll)
        self.previous=QPushButton(self);self.previous.setObjectName('railEdge');self.previous.setIcon(icon('left','#ffffff'));self.previous.setToolTip('Anterior')
        self.next=QPushButton(self);self.next.setObjectName('railEdge');self.next.setIcon(icon('right','#ffffff'));self.next.setToolTip('Siguiente')
        self.previous.clicked.connect(lambda:self.move_by(-1));self.next.clicked.connect(lambda:self.move_by(1))
        bar=self.scroll.horizontalScrollBar();bar.valueChanged.connect(self.update_buttons);bar.rangeChanged.connect(self.update_buttons)
    def move_by(self,direction):
        bar=self.scroll.horizontalScrollBar();bar.setValue(bar.value()+direction*max(180,self.width()-180))
    def update_buttons(self,*args):
        bar=self.scroll.horizontalScrollBar();self.previous.setVisible(bar.value()>bar.minimum());self.next.setVisible(bar.value()<bar.maximum());self.previous.raise_();self.next.raise_()
    def resizeEvent(self,event):
        super().resizeEvent(event);height=min(90,max(44,self.height()-24));y=(self.height()-height)//2
        self.previous.setGeometry(0,y,44,height);self.next.setGeometry(max(0,self.width()-44),y,44,height);self.update_buttons()

class ProfileAvatar(Poster):
    def paintEvent(self,event):
        p=QPainter(self);p.setRenderHint(QPainter.RenderHint.Antialiasing)
        rect=QRectF(self.rect()).adjusted(5,5,-5,-5);path=QPainterPath();path.addEllipse(rect);p.setClipPath(path);p.fillRect(rect,QColor('#242428'))
        if not self.pix.isNull():
            pix=self.pix.scaled(self.size()-QSize(10,10),Qt.AspectRatioMode.KeepAspectRatioByExpanding,Qt.TransformationMode.SmoothTransformation)
            p.drawPixmap((self.width()-pix.width())//2,(self.height()-pix.height())//2,pix)
        p.setClipping(False);p.setPen(QColor('#f1f1f1' if self.underMouse() or self.hasFocus() else '#45454a'));p.drawEllipse(rect);p.end()


class Starfield(QWidget):
    """Paint-only profile backdrop, no external image or dependencies.

    Keeps overlays and click targets native Qt widgets, including when offline.
    """
    def __init__(self,parent=None):
        super().__init__(parent)
        self.setMinimumHeight(545)
        self.setAttribute(Qt.WidgetAttribute.WA_StyledBackground,False)
    def paintEvent(self,event):
        p=QPainter(self);p.setRenderHint(QPainter.RenderHint.Antialiasing)
        w,h=self.width(),self.height()
        bg=QLinearGradient(0,0,w,h)
        bg.setColorAt(0,QColor('#050b19'));bg.setColorAt(.46,QColor('#12172d'))
        bg.setColorAt(1,QColor('#070d1d'))
        p.fillRect(self.rect(),bg)
        # A cool violet planet glow above a layered, midnight landscape.
        halo=QRadialGradient(QPointF(w*.52,h*.30),w*.48)
        halo.setColorAt(0,QColor(104,90,180,90))
        halo.setColorAt(1,QColor(40,50,110,0))
        p.fillRect(self.rect(),halo)
        p.setPen(Qt.PenStyle.NoPen)
        radius=min(w*.20,h*.33)
        planet=QRadialGradient(QPointF(w*.49,h*.26),radius)
        planet.setColorAt(0,QColor(66,89,155,105))
        planet.setColorAt(.55,QColor(47,65,119,75))
        planet.setColorAt(1,QColor(31,42,81,0))
        p.setBrush(planet)
        p.drawEllipse(QPointF(w*.49,h*.26),radius,radius)
        for i in range(130):
            x=((i*1837+97)%997)/997*w;y=((i*703+31)%997)/997*h*.68
            c=QColor(195,205,255,38+(i%5)*23)
            p.setBrush(c);r=0.55+(i%4)*.27;p.drawEllipse(QPointF(x,y),r,r)
        def mountain(peaks,color):
            poly=QPolygonF([QPointF(0,h)]+[QPointF(x*w,y*h) for x,y in peaks]+[QPointF(w,h)])
            p.setBrush(QColor(color));p.drawPolygon(poly)
        mountain([(0,.72),(.11,.60),(.24,.74),(.38,.56),(.53,.72),(.68,.60),(.78,.68),(.91,.54),(1,.69)],'#17233c')
        mountain([(0,.81),(.13,.69),(.21,.83),(.34,.67),(.51,.85),(.66,.73),(.78,.82),(.92,.68),(1,.79)],'#0c172b')
        p.end()
