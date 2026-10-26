"""Create updater metadata only after Android tools verify the APK and its permanent certificate."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

root=Path(__file__).resolve().parents[1]
apk=Path(sys.argv[1]).resolve()
config=(root/'mobile/build.gradle').read_text()
version=re.search(r"versionName\s+'([^']+)'",config).group(1)
code=int(re.search(r'versionCode\s+(\d+)',config).group(1))
sdk=Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0'
verified=subprocess.check_output([str(sdk/'apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True)
certs=re.findall(r'Signer #\d+ certificate SHA-256 digest:\s*([A-Fa-f0-9]+)',verified)
expected=(root/'signing_certificate_sha256.txt').read_text().strip().lower()
if len(certs)!=1 or certs[0].lower()!=expected:
    raise SystemExit('APK certificate does not match the permanent signing certificate.')
badging=subprocess.check_output([str(sdk/'aapt'),'dump','badging',str(apk)],text=True)
package=re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",badging)
if not package or package.groups()!=('com.brunio.mobile',str(code),version):
    raise SystemExit('APK package/version differs from the declared release.')
name=f'BruniO-Mobile-v{version}.apk'
shutil.copyfile(apk,root/name)
shutil.copyfile(apk,root/'BruniO-Mobile.apk')
repo=os.environ.get('GITHUB_REPOSITORY','randy3m2/miflix-tv-native')
metadata={'applicationId':'com.brunio.mobile','version':version,'versionCode':code,'downloadUrl':f'https://github.com/{repo}/releases/download/mobile-v{version}/{name}',
          'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'sizeBytes':apk.stat().st_size}
(root/'update.json').write_text(json.dumps(metadata,indent=2)+'\n')
print(f'Verified stable certificate and package; prepared {version} / {code}.')
