import importlib.util,unittest
from pathlib import Path
from unittest.mock import patch
spec=importlib.util.spec_from_file_location('update_bridge_test',Path(__file__).resolve().parents[1]/'backend'/'bridge.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
import pc_updates
class ChannelTests(unittest.TestCase):
    def test_github_owner_casing_does_not_break_valid_pc_update(self):
        release={'assets':[{'name':'pc-update.json','browser_download_url':'https://github.com/Randy3m2/miflix-tv-native/releases/download/pc-latest/pc-update.json'}]}
        info={'channel':'pc-windows-x64','version':'2.0.0-rc99','sha256':'a'*64,'sizeBytes':100,'downloadUrl':'https://github.com/randy3m2/miflix-tv-native/releases/download/pc-v2.0.0-rc99/BruniO-PC-Setup-2.0.0-rc99.exe'}
        with patch.object(pc_updates,'get_json',side_effect=[release,info]) as request:
            self.assertEqual(pc_updates.get_latest(),info);self.assertEqual(request.call_count,2)
    def test_other_repository_never_fetches_update_metadata(self):
        release={'assets':[{'name':'pc-update.json','browser_download_url':'https://github.com/other/repository/releases/download/pc-latest/pc-update.json'}]}
        with patch.object(pc_updates,'get_json',return_value=release) as request:
            with self.assertRaises(ValueError):pc_updates.get_latest()
            self.assertEqual(request.call_count,1)
    def test_channel_is_pc_only_even_with_correct_metadata_location(self):
        release={'assets':[{'name':'pc-update.json','browser_download_url':'https://github.com/Randy3m2/miflix-tv-native/releases/download/pc-latest/pc-update.json'}]}
        with patch.object(pc_updates,'get_json',side_effect=[release,{'channel':'tv'}]):
            with self.assertRaises(ValueError):pc_updates.get_latest()
if __name__=='__main__':unittest.main()
