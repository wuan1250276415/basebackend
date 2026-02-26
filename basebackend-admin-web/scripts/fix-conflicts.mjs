import fs from 'fs';
import path from 'path';

function findFiles(dir, exts) {
    let results = [];
    const list = fs.readdirSync(dir);
    list.forEach(file => {
        file = path.join(dir, file);
        if (fs.statSync(file).isDirectory()) results = results.concat(findFiles(file, exts));
        else if (exts.some(ext => file.endsWith(ext))) results.push(file);
    });
    return results;
}

const files = findFiles('src', ['.ts', '.tsx']);

const replacements = {
    'Upload': 'UploadCloud',
    'Layout': 'PanelTop',
    'Menu': 'AlignJustify',
    'Image': 'ImageIcon',
    'Link': 'Link2'
};

files.forEach(file => {
    let content = fs.readFileSync(file, 'utf8');
    let changed = false;

    const lucideImportRegex = /import\s+\{([^}]+)\}\s+from\s+['\"]lucide-react['\"]/;
    const match = lucideImportRegex.exec(content);

    if (match) {
        let imports = match[1].split(',').map(s => s.trim()).filter(Boolean);
        let toReplace = {};

        imports = imports.map(i => {
            if (replacements[i]) {
                toReplace[i] = replacements[i];
                changed = true;
                return replacements[i];
            }
            return i;
        });

        if (changed) {
            content = content.replace(lucideImportRegex, `import { ${Array.from(new Set(imports)).join(', ')} } from 'lucide-react'`);

            for (const [oldName, newName] of Object.entries(toReplace)) {
                // Replace exact lucide references:
                content = content.replace(new RegExp(`<${oldName}\\\\s+size=\\\\{16\\\\}`, 'g'), `<${newName} size={16}`);
                content = content.replace(new RegExp(`<${oldName}\\\\s+/>`, 'g'), `<${newName} />`);
                content = content.replace(new RegExp(`icon=\\\\{${oldName}\\\\}`, 'g'), `icon={${newName}}`);
                content = content.replace(new RegExp(`icon:\\\\s+${oldName}`, 'g'), `icon: ${newName}`);
            }
            fs.writeFileSync(file, content);
            console.log('Fixed conflicts in', file);
        }
    }
});
