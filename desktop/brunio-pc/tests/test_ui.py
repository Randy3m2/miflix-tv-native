"""Real Qt widgets offscreen; fixtures avoid production network/account mutations."""
import os,time,unittest
os.environ.setdefault('QT_QPA_PLATFORM','offscreen')
from PySide6.QtWidgets import QApplication,QLineEdit,QPushButton,QWidget,QHBoxLayout
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
        self.w.search();self.assertTrue(self.w.stack.currentWidget().findChildren(QLineEdit));self.w.settings(refresh=False);self.assertTrue(self.w.stack.currentWidget().findChildren(QPushButton));self.w.profiles(manage=True);self.app.processEvents()
        self.assertTrue(any(b.text()=='Avatar' for b in self.w.stack.currentWidget().findChildren(QPushButton)))
    def test_pages_are_top_aligned_and_live_uses_tv_playlist(self):
        self.w.image=lambda *args,**kwargs:None
        self.w.resize(1000,700);self.w.show();self.w.live()
        deadline=time.monotonic()+3
        while not self.w.live_candidates and time.monotonic()<deadline:self.app.processEvents();time.sleep(.01)
        self.assertGreater(len(self.w.live_candidates),100)
        self.assertEqual(self.w.live_candidates[0][1],'tv')
        page=self.w.stack.currentWidget();self.assertLessEqual(page.horizontalScrollBar().maximum(),0)
        self.w.search();self.app.processEvents();page=self.w.stack.currentWidget()
        inputs=page.findChildren(QLineEdit);self.assertTrue(inputs);self.assertLess(inputs[0].mapTo(page,inputs[0].rect().topLeft()).y(),300)
    def test_profile_preferences_and_retry_are_scoped(self):
        self.w.client.profile='one';self.w.save_profile_pref('retry_links',False)
        self.w.client.profile='two';self.assertTrue(self.w.profile_pref('retry_links',True))
        self.w.client.profile='one';self.assertFalse(self.w.profile_pref('retry_links',True))
    def test_rc6_screenshot_one_has_sidebar_and_search_filters(self):
        self.assertEqual(self.w.sidebar.objectName(),'sidebar')
        self.assertTrue({'home','search','movies','series','list','collections'}.issubset(self.w.nav_buttons))
        self.w.search();self.app.processEvents()
        self.assertTrue(self.w.stack.currentWidget().findChild(QLineEdit,'searchInput'))
        pills=[b.text() for b in self.w.stack.currentWidget().findChildren(QPushButton,'filterPill')]
        self.assertIn('Actores',pills)
        self.assertIn('Géneros',pills)
    def test_rc6_cinematic_profiles_and_detail(self):
        self.w.profiles();self.app.processEvents()
        from brunio.visuals import Starfield
        self.assertTrue(self.w.stack.currentWidget().findChildren(Starfield))
        self.w.image=lambda *args,**kwargs:None
        movie={'id':42,'type':'movie','cloudId':'tmdb:movie:42','title':'A sample film','overview':'A story', 'year':'2026','rating':8,'backdrop':''}
        self.w.open(movie);self.app.processEvents()
        self.assertTrue(self.w.stack.currentWidget().findChild(QWidget,'detailHero'))
        self.assertFalse(self.w.sidebar.isHidden())
    def test_source_retry_keeps_position_and_respects_party_and_toggle(self):
        class FakePlayer:
            def get_time(self):return 42000
            def stop(self):pass
            def release(self):pass
        self.w.player=FakePlayer();self.w.current={'item':{'type':'movie','title':'test'},'season':0,'episode':0,'stream':{'url':'https://a'}}
        self.w.sources=[{'url':'https://a'},{'url':'https://b'}];self.w.fallback_urls={'https://a'};self.w.source_last_pos=50000
        calls=[];self.w.play_stream=lambda *args:calls.append(args)
        self.assertTrue(self.w.retry_next_source());self.assertEqual(calls[0][3],50000);self.assertEqual(calls[0][4]['url'],'https://b')
        self.w.client.party={'room_code':'123456'};self.assertFalse(self.w.retry_next_source());self.w.client.party=None
        self.w.save_profile_pref('retry_links',False);self.assertFalse(self.w.retry_next_source());self.w.player=None
    def test_profiles_hide_header_and_settings_expose_update(self):
        self.w.show();self.w.profiles();self.app.processEvents();self.assertTrue(self.w.sidebar.isHidden())
        self.w.settings(False);self.app.processEvents();self.assertFalse(self.w.sidebar.isHidden())
        self.assertTrue(self.w.stack.currentWidget().findChild(QPushButton,'updateButton'))
    def test_edge_rail_arrows_scroll_the_actual_content(self):
        from brunio.visuals import EdgeRail
        content=QWidget();layout=QHBoxLayout(content)
        for i in range(12):
            b=QPushButton(str(i));b.setFixedWidth(150);layout.addWidget(b)
        rail=EdgeRail(content,100);rail.resize(600,100);rail.show();self.app.processEvents()
        bar=rail.scroll.horizontalScrollBar();self.assertGreater(bar.maximum(),0)
        rail.move_by(1);self.assertGreater(bar.value(),0)
        rail.move_by(-1);self.assertEqual(bar.value(),0);rail.close()
    def test_hover_sidebar_overlays_without_resizing_content(self):
        self.w.resize(1000,700);self.w.show();self.app.processEvents();self.w.set_sidebar_expanded(False);self.app.processEvents()
        before=self.w.content.geometry();self.assertEqual(self.w.sidebar.width(),76)
        self.w.set_sidebar_expanded(True);self.app.processEvents();self.assertEqual(self.w.sidebar.width(),224);self.assertEqual(self.w.content.geometry(),before)
        self.assertTrue(self.w.nav_buttons['home'].text());self.w.set_sidebar_expanded(False);self.assertEqual(self.w.nav_buttons['home'].text(),'')
    def test_local_platform_gif_stays_stopped_until_hover(self):
        from brunio.visuals import PlatformTile
        from PySide6.QtGui import QMovie
        tiny_gif=bytes.fromhex('47494638396101000100800000000000ffffff21f90401000000002c00000000010001000002024401003b')
        tile=PlatformTile();tile.setAnimation(tiny_gif)
        self.assertIsNotNone(tile.movie);self.assertEqual(tile.movie.state(),QMovie.MovieState.NotRunning)
    def test_home_hero_does_not_wait_for_slow_shelves(self):
        import threading
        started=threading.Event();release=threading.Event()
        row={'id':8,'type':'movie','cloudId':'tmdb:movie:8','title':'Fixture movie','overview':'','year':'2026','rating':8,'backdrop':'','poster':''}
        def catalog(path,*args,**kwargs):
            if path=='/movie/popular':started.set();release.wait(2);return []
            return [row] if path=='/trending/all/week' else []
        self.w.client.catalog=catalog;self.w.image=lambda *args,**kwargs:None;self.w.show();self.w.home()
        try:
            self.assertTrue(started.wait(1));deadline=time.monotonic()+1
            while time.monotonic()<deadline:
                self.app.processEvents();headline=self.w.stack.currentWidget().findChild(QWidget,'heroHeadline')
                if headline and headline.text()=='Fixture movie':break
                time.sleep(.01)
            self.assertIsNotNone(headline);self.assertEqual(headline.text(),'Fixture movie');self.assertFalse(release.is_set())
        finally:release.set()
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
