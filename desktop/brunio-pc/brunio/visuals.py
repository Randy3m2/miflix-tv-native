"""Native Qt visual components; no web view or extra runtime required."""
from PySide6.QtCore import Qt, QSize, QRectF, QByteArray
from PySide6.QtGui import QPixmap, QIcon, QPainter, QPainterPath, QColor, QLinearGradient
from PySide6.QtSvg import QSvgRenderer
from PySide6.QtWidgets import QWidget, QPushButton

PATHS={
 'home':'<path d="M3 11 12 3l9 8v10h-6v-7H9v7H3Z"/>',
 'search':'<circle cx="10" cy="10" r="7"/><path d="m15 15 6 6"/>',
 'collections':'<rect x="3" y="6" width="18" height="15" rx="3"/><path d="M7 3h10M3 11h18"/>',
 'friends':'<circle cx="9" cy="8" r="4"/><path d="M2 21v-3a7 7 0 0 1 14 0v3M17 5a4 4 0 0 1 0 8M19 16a5 5 0 0 1 3 5"/>',
 'live':'<rect x="2" y="6" width="20" height="15" rx="3"/><path d="m8 2 4 4 4-4m-6 14 5-3-5-3Z"/>',
 'list':'<path d="M5 3h14v19l-7-5-7 5Z"/>',
 'profiles':'<circle cx="12" cy="8" r="4"/><path d="M3 22v-3a9 9 0 0 1 18 0v3"/>',
 'settings':'<path d="m9 2-1 4-4 1-2 5 3 3v5l5 2 3-3 5 1 3-5-2-4 1-5-5-2-3 3Z"/><circle cx="12" cy="12" r="3"/>',
 'bell':'<path d="M4 18h16l-2-4V9a6 6 0 0 0-12 0v5ZM9 21h6"/>',
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
        path=QPainterPath();path.addRoundedRect(QRectF(self.rect()),16,16);p.setClipPath(path);p.fillRect(self.rect(),QColor('#19191b'))
        if not self.pix.isNull():
            scaled=self.pix.scaled(self.size(),Qt.AspectRatioMode.KeepAspectRatioByExpanding,Qt.TransformationMode.SmoothTransformation)
            p.drawPixmap((self.width()-scaled.width())//2,(self.height()-scaled.height())//2,scaled)
        if self.hero:
            gradient=QLinearGradient(0,0,self.width(),0);gradient.setColorAt(0,QColor(8,8,9,245));gradient.setColorAt(.6,QColor(8,8,9,165));gradient.setColorAt(1,QColor(8,8,9,35));p.fillRect(self.rect(),gradient)
        p.end()

class Poster(QPushButton):
    def __init__(self,parent=None):super().__init__(parent);self.pix=QPixmap();self.setObjectName('poster')
    def setPixmap(self,pix):self.pix=pix;self.update()
    def paintEvent(self,event):
        p=QPainter(self);p.setRenderHint(QPainter.RenderHint.Antialiasing)
        rect=QRectF(self.rect()).adjusted(3,3,-3,-3);path=QPainterPath();path.addRoundedRect(rect,12,12);p.setClipPath(path);p.fillRect(rect,QColor('#19191b'))
        if not self.pix.isNull():
            pix=self.pix.scaled(self.size()-QSize(6,6),Qt.AspectRatioMode.KeepAspectRatioByExpanding,Qt.TransformationMode.SmoothTransformation)
            p.drawPixmap((self.width()-pix.width())//2,(self.height()-pix.height())//2,pix)
        p.setClipping(False)
        if self.underMouse() or self.hasFocus():p.setPen(QColor('#ffffff'));p.drawRoundedRect(QRectF(self.rect()).adjusted(1,1,-2,-2),14,14)
        p.end()
    def enterEvent(self,e):self.update();super().enterEvent(e)
    def leaveEvent(self,e):self.update();super().leaveEvent(e)
