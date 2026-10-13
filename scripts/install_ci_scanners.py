"""Download pinned official Linux scanners and verify the publishers' digests before execution."""
import hashlib, json, os, stat, tarfile, urllib.request
from pathlib import Path

destination=Path('.local/security-tools');destination.mkdir(parents=True,exist_ok=True)
def fetch(url):
    request=urllib.request.Request(url,headers={'User-Agent':'TechZone-CI','Accept':'application/vnd.github+json'})
    token=os.environ.get('GH_TOKEN')
    if token and url.startswith('https://api.github.com/'): request.add_header('Authorization','Bearer '+token)
    with urllib.request.urlopen(request,timeout=60) as response: return response.read()
def release(repo, tag): return json.loads(fetch(f'https://api.github.com/repos/{repo}/releases/tags/{tag}'))
def asset(release, name): return next(a for a in release['assets'] if a['name']==name)
gitleaks=release('gitleaks/gitleaks','v8.30.1')
archive_name='gitleaks_8.30.1_linux_x64.tar.gz'
archive=fetch(asset(gitleaks,archive_name)['browser_download_url'])
checksums=fetch(asset(gitleaks,'gitleaks_8.30.1_checksums.txt')['browser_download_url']).decode()
expected=next(line.split()[0] for line in checksums.splitlines() if line.split()[-1]==archive_name)
if hashlib.sha256(archive).hexdigest()!=expected: raise SystemExit('Gitleaks checksum mismatch')
archive_path=destination/archive_name;archive_path.write_bytes(archive)
with tarfile.open(archive_path) as tar:
    member=next(m for m in tar.getmembers() if m.name=='gitleaks' and m.isfile())
    (destination/'gitleaks').write_bytes(tar.extractfile(member).read())
osv=asset(release('google/osv-scanner','v2.6.0'),'osv-scanner_linux_amd64')
binary=fetch(osv['browser_download_url']);expected=osv.get('digest')
if not expected or expected!='sha256:'+hashlib.sha256(binary).hexdigest(): raise SystemExit('OSV publisher digest missing or mismatched')
(destination/'osv-scanner').write_bytes(binary)
for name in ('gitleaks','osv-scanner'):
    path=destination/name;path.chmod(path.stat().st_mode|stat.S_IXUSR)
print('Verified Gitleaks 8.30.1 and OSV Scanner 2.6.0')
