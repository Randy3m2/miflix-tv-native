const fs=require('fs'),assert=require('assert');
const base='app/src/main/java/com/miflix/native2/ui/';
const focus=fs.readFileSync(base+'SmoothRowFocus.kt','utf8');
const entry=focus.slice(focus.indexOf('internal fun Modifier.firstCardOnDown'));
assert(!entry.includes('cancelFocusChange()'),'Down entry must finish normal focus search');
assert(entry.includes('LaunchedEffect(active,enteredDown)'));
assert(entry.includes('if(!active || !enteredDown) return@LaunchedEffect'));
assert(entry.indexOf('row.scrollToItem(0)')<entry.indexOf('first.requestFocus()'));
assert(entry.includes('finally {enteredDown=false}'));
const home=fs.readFileSync(base+'Screens.kt','utf8');
assert(home.includes('headerWork.forEach {it.cancel()}'));
assert(home.match(/cancelHeaderWork\(\)/g).length>=3);
assert(home.includes('if(homeReturn.pending==null)'));
for(const name of ['SmoothRowFocus.kt','Screens.kt','UpdateNotes.kt']) {
 assert(fs.readFileSync(base+name).equals(fs.readFileSync('main/java/com/miflix/native2/ui/'+name)),name+' mirror');
}
console.log('PASS: committed Down focus, bounded first-card retries, cancelled stale header jobs, return-position guard and mirrors. Source checks only; TV validation pending.');
