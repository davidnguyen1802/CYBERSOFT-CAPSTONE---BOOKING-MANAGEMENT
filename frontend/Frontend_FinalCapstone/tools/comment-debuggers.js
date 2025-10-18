const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..', 'src');

function walk(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full);
    } else if (entry.isFile() && /\.(ts|js)$/.test(entry.name)) {
      commentDebuggers(full);
    }
  }
}

function commentDebuggers(filePath) {
  let content = fs.readFileSync(filePath, 'utf8');
  const original = content;
  // Replace lines that contain only optional whitespace + debugger + optional semicolon + optional whitespace
  content = content.replace(/(^[ \t]*)(debugger;?)[ \t]*$/gm, '$1// $2');
  if (content !== original) {
    fs.writeFileSync(filePath, content, 'utf8');
    console.log('Updated:', filePath);
  }
}

try {
  walk(root);
  console.log('Done.');
} catch (err) {
  console.error('Error:', err.message);
  process.exit(1);
}
