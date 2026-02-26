import fs from 'fs';
import path from 'path';

function findFiles(dir, exts) {
    let results = [];
    const list = fs.readdirSync(dir);
    list.forEach(file => {
        file = path.join(dir, file);
        const stat = fs.statSync(file);
        if (stat && stat.isDirectory()) {
            results = results.concat(findFiles(file, exts));
        } else {
            exts.forEach(ext => {
                if (file.endsWith(ext)) results.push(file);
            });
        }
    });
    return results;
}

const files = findFiles('src', ['.ts', '.tsx']);
const icons = new Set();

files.forEach(file => {
    const content = fs.readFileSync(file, 'utf8');
    // Match single line imports
    const regex = /import\s+\{([^}]+)\}\s+from\s+['\"]@ant-design\/icons['\"]/g;
    let match;
    while ((match = regex.exec(content)) !== null) {
        const imports = match[1].split(',').map(s => s.trim()).filter(Boolean);
        imports.forEach(i => icons.add(i));
    }

    // Match multiline imports
    const multiRegex = /import\s+\{([^]*?)\}\s+from\s+['\"]@ant-design\/icons['\"]/g;
    while ((match = multiRegex.exec(content)) !== null) {
        const imports = match[1].split(',').map(s => s.trim()).filter(Boolean);
        imports.forEach(i => icons.add(i));
    }
});

console.log(Array.from(icons).sort().join('\n'));
