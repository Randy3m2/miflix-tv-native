"""Real Qt widgets offscreen; fixtures avoid production network/account mutations."""
import os,time,unittest
os.environ.setdefault('QT_QPA_PLATFORM','offscreen')
from PySide6.QtWidgets import QApplication,QLineEdit,QPushButton,QWidget,QHBoxLayout,QVBoxLayout
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
    def test_rc7_reference_components_and_secondary_navigation(self):
        from brunio.ui import PLATFORM_ASSETS
        from brunio.visuals import ContinueCard,PlatformTile
        self.assertTrue((PLATFORM_ASSETS/'netflix.png').is_file())
        self.assertTrue((PLATFORM_ASSETS/'disney-plus.png').is_file())
        self.assertTrue(self.w.extra_nav.isHidden())
        self.w.toggle_more();self.assertFalse(self.w.extra_nav.isHidden())
        self.w.toggle_more();self.assertTrue(self.w.extra_nav.isHidden())
        self.w.select_nav('live');self.assertFalse(self.w.extra_nav.isHidden())
        self.assertIsInstance(PlatformTile('Netflix'),QPushButton)
        resume=ContinueCard('A movie',62)
        self.assertAlmostEqual(resume.progress,.62)
        resume.setProgress(18);self.assertAlmostEqual(resume.progress,.18)
        self.w.image=lambda *args,**kwargs:None
        self.w.state={'progress':{'tmdb:movie:42':{'percent':72,'position':30}}}
        host=QWidget();layout=QVBoxLayout(host)
        item={'id':42,'cloudId':'tmdb:movie:42','title':'Sample','type':'movie','backdrop':''}
        self.w.continue_rail(layout,[item])
        entries=host.findChildren(ContinueCard)
        self.assertEqual(len(entries),1)
        self.assertAlmostEqual(entries[0].progress,.72)

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
