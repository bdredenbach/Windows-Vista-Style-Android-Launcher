"""Keep the tested Vista desktop intact outside the requested setup integration."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parent.parent
BASE = '8f6580d0772f9edba21704f6c5b369718f961ab9'
approved = {
    'app/src/main/java/com/nthexperience/desktop/InitializationFlow.java',
    'app/src/main/java/com/nthexperience/desktop/InitializationState.java',
    'app/src/main/java/com/nthexperience/desktop/MainActivity.java',
    'app/src/main/java/com/nthexperience/desktop/StartupExperience.java',
    'app/src/main/java/com/nthexperience/desktop/UsageAccessExample.java',
}
changed = set(subprocess.check_output(['git','diff','--name-only',BASE,'--','app/src/main'],cwd=ROOT,text=True).splitlines())
assert changed <= approved, 'Unrelated Vista runtime changes: ' + str(changed - approved)
build = (ROOT / 'app/build.gradle').read_text()
assert "applicationId 'com.nthexperience.desktop'" in build
assert "versionName '0.26.3'" in build and 'versionCode 40' in build
startup = (ROOT / 'app/src/main/java/com/nthexperience/desktop/StartupExperience.java').read_text()
old = subprocess.check_output(['git','show',BASE+':app/src/main/java/com/nthexperience/desktop/StartupExperience.java'],cwd=ROOT,text=True)
assert startup[startup.index('    private void show(boolean preview)'):] == old[old.index('    private void show(boolean preview)'):], 'Vista introduction artwork changed'
print('PASS: Vista package/version, original introduction artwork and all runtime files outside the five approved setup paths are preserved')
