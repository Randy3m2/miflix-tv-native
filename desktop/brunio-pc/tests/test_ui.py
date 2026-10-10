"""Real Qt widgets offscreen; fixtures avoid production network/account mutations."""
import os,time,unittest
os.environ.setdefault('QT_QPA_PLATFORM','offscreen')
from PySide6.QtWidgets import QApplication,QLineEdit,QPushButton
from brunio.ui import Window,java_hash
class UITests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):cls.app=QApplication.instance() or QApplication([])
    def setUp(self):
        self.w=Window(smoke=True);self.w.tick_timer.stop();self.w.social_timer.stop()
        self.w.client.session={'userId':'fixture','accessToken':'fixture'}
        self.w.client.account={'profiles':[{'id':'default','name':'Main','avatarValue':'ai:fox','primary':True}]}
        self.w.persist=lambda:None
        self.w.client.transport=lambda *args,**kwargs:[]
    def tearDown(self):self.w.client.session={};self.w.close();self.w.pool.waitForDone(3000)
    def test_search_and_settings_widgets_survive_navigation(self):
        self.w.search();self.assertTrue(self.w.stack.currentWidget().findChildren(QLineEdit));self.w.settings(refresh=False);self.assertTrue(self.w.stack.currentWidget().findChildren(QPushButton));self.w.profiles();self.app.processEvents()
        self.assertTrue(any(b.text()=='Avatar' for b in self.w.stack.currentWidget().findChildren(QPushButton)))
    def test_tv_live_hash_matches_kotlin_java(self):
        self.assertEqual(java_hash('abc'),96354);self.assertEqual(java_hash(''),0)
    def test_sidebar_selection_and_small_window_margins(self):
        self.w.resize(900,640);self.w.show();self.w.select_nav('search');self.app.processEvents()
        self.assertTrue(self.w.nav_buttons['search'].property('selected'))
        self.assertFalse(self.w.nav_buttons['home'].property('selected'))
        self.assertLess(self.w.alert_button.geometry().bottom(),self.w.sidebar.height())
    def test_settings_refresh_does_not_replace_later_search_page(self):
        self.w.settings();self.w.search();page=self.w.stack.currentWidget()
        self.w.pool.waitForDone(3000);self.app.processEvents()
        self.assertIs(self.w.stack.currentWidget(),page)
    def test_ui_workers_apply_results_on_main_thread(self):
        values=[];self.w.run(lambda:42,values.append)
        deadline=time.monotonic()+3
        while not values and time.monotonic()<deadline:self.app.processEvents();time.sleep(.01)
        self.assertEqual(values,[42])
    def test_old_worker_cannot_mutate_screen_after_leave(self):
        import threading
        started=threading.Event();release=threading.Event();values=[]
        def task():started.set();release.wait(2);return 'late'
        self.w.run(task,values.append);self.assertTrue(started.wait(1));self.w.client.detach();release.set();self.w.pool.waitForDone(3000);self.app.processEvents();self.assertEqual(values,[])
