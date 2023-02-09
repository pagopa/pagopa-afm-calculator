const  IGNORED_FILES  = 'openapi.json, src/'
var changes = 1000;

const ignored_files = IGNORED_FILES.trim().split(',').filter(word => word.length > 0);
if (ignored_files.length > 0){
  var ignored = 0
  const execSync = require('child_process').execSync;
  for (const file of IGNORED_FILES.trim().split(',')) {

    const ignored_additions_str = execSync('git --no-pager  diff --numstat main..head | grep ' + file + ' | cut -f 1', { encoding: 'utf-8' })
    const ignored_deletions_str = execSync('git --no-pager  diff --numstat main..head | grep ' + file + ' | cut -f 2', { encoding: 'utf-8' })

    const ignored_additions = ignored_additions_str.split('\n').map(elem=> parseInt(elem || 0)).reduce(
          (accumulator, currentValue) => accumulator + currentValue,
          0);
    const ignored_deletions = ignored_deletions_str.split('\n').map(elem=> parseInt(elem || 0)).reduce(
          (accumulator, currentValue) => accumulator + currentValue,
          0);

    ignored += ignored_additions + ignored_deletions;
  }
  changes -= ignored
  console.log('ignored lines: ' + ignored + ' , consider changes: ' + changes);
}
