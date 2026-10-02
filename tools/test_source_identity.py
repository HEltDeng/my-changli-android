"""Identity integration tests. All mutations are confined to a disposable fixture clone.
Usage: python tools/test_source_identity.py --scratch <explicit empty directory>
"""
import argparse,json,subprocess,unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
def run(args,cwd=None):
    return subprocess.run([str(x) for x in args],cwd=cwd,capture_output=True,text=True,encoding='utf-8',errors='replace')
def git(*args):
    r=run(['git','-C',FIXTURE,*args]);assert r.returncode==0,r.stderr;return r.stdout.strip()

class IdentityTests(unittest.TestCase):
    def setUp(self):
        git('checkout','--detach',START)
        git('remote','set-url','origin','https://github.com/HEltDeng/my-changli-android.git')
        marker=FIXTURE/'app/build.gradle.kts'
        marker.write_bytes(ORIGINAL_MARKER)
    def check(self,expected=True,repo=None,mode='Validation',extra=(),cwd=None):
        r=run(['powershell.exe','-NoProfile','-ExecutionPolicy','Bypass','-File',ROOT/'tools/Test-MyChangliSourceIdentity.ps1','-Repository',repo or FIXTURE,'-Mode',mode,*extra],cwd=cwd)
        self.assertEqual(r.returncode==0,expected,r.stdout+r.stderr)
    def test_registered_entry(self):
        self.check(repo=ROOT,mode='Development',extra=['-LocalRegistry',REGISTRY])
    def test_full_independent_fixture(self):self.check()
    def test_ssh_scp(self):
        git('remote','set-url','origin','git@github.com:HEltDeng/my-changli-android.git');self.check()
    def test_ssh_url(self):
        git('remote','set-url','origin','ssh://git@github.com/HEltDeng/my-changli-android.git');self.check()
    def test_descendant(self):
        git('-c','user.name=Identity fixture','-c','user.email=fixture@invalid','commit','--allow-empty','-m','test: isolated descendant identity');self.check()
    def test_relative_trailing_separator_and_spaces(self):self.check(repo=FIXTURE.name+'\\',cwd=SCRATCH)
    def test_wrong_owner(self):
        git('remote','set-url','origin','https://github.com/OtherOwner/my-changli-android.git');self.check(False)
    def test_wrong_repository(self):
        git('remote','set-url','origin','https://github.com/HEltDeng/my-chuannong-android.git');self.check(False)
    def test_wrong_product(self):
        p=FIXTURE/'app/build.gradle.kts';p.write_bytes(ORIGINAL_MARKER.replace(b'com.mychangli.app',b'com.wrong.app'));self.check(False)
    def test_pre_release_line(self):
        git('checkout','--detach',BASELINE+'^');self.check(False)
    def test_unregistered_directory(self):self.check(False,mode='Development',extra=['-LocalRegistry',REGISTRY])
    def test_missing_git(self):self.check(False,repo=SCRATCH)
    def test_nonexistent_path(self):self.check(False,repo=SCRATCH/'missing')
    def test_forbidden_historical_entry(self):
        forbidden=SCRATCH/'my-changli-android-current';forbidden.mkdir(exist_ok=True);self.check(False,repo=forbidden)
    def test_missing_baseline_shallow_history(self):
        shallow=SCRATCH/'shallow';r=run(['git','clone','--depth','1',FIXTURE.as_uri(),shallow]);self.assertEqual(r.returncode,0,r.stderr)
        r=run(['git','-C',shallow,'remote','set-url','origin','https://github.com/HEltDeng/my-changli-android.git']);self.assertEqual(r.returncode,0)
        self.check(False,repo=shallow)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--scratch',required=True);opts=parser.parse_args()
    SCRATCH=Path(opts.scratch).resolve();assert not SCRATCH.exists(),'Choose a new empty fixture directory'
    SCRATCH.mkdir(parents=True);FIXTURE=SCRATCH/'legitimate clone with spaces'
    r=run(['git','clone','--no-local',ROOT,FIXTURE]);assert r.returncode==0,r.stderr
    assert not (FIXTURE/'.git/objects/info/alternates').exists()
    START=git('rev-parse','HEAD');BASELINE=json.loads((ROOT/'tools/source-identity.json').read_text('utf-8-sig'))['release']['commit']
    ORIGINAL_MARKER=(FIXTURE/'app/build.gradle.kts').read_bytes()
    REGISTRY=SCRATCH/'local-registry.json';REGISTRY.write_text(json.dumps({'schemaVersion':1,'repositories':{'android':str(ROOT)}}),encoding='utf-8')
    unittest.main(argv=['identity-tests'],verbosity=2)
