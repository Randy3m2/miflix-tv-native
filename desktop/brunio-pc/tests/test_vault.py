import os,tempfile,unittest
from pathlib import Path
from brunio.vault import Vault
@unittest.skipUnless(os.name=='nt','Windows DPAPI only')
class VaultTests(unittest.TestCase):
    def test_roundtrip_and_tokens_not_plaintext(self):
        with tempfile.TemporaryDirectory() as tmp:
            vault=Vault();vault.path=Path(tmp)/'account.dat';data={'session':{'accessToken':'private-fixture-token'}}
            vault.save(data);self.assertEqual(vault.load(),data);self.assertNotIn(b'private-fixture-token',vault.path.read_bytes());vault.clear();self.assertEqual(vault.load(),{})
    def test_corrupt_ciphertext_does_not_restore_account(self):
        with tempfile.TemporaryDirectory() as tmp:
            vault=Vault();vault.path=Path(tmp)/'account.dat';vault.path.write_bytes(b'corrupt');self.assertEqual(vault.load(),{})
