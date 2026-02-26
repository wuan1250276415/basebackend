import fs from 'fs';
import path from 'path';

const filesToFix = [
    'src/pages/Workflow/TaskManagement/MyInitiated.tsx',
    'src/pages/Workflow/ProcessInstance/index.tsx',
    'src/pages/Workflow/ProcessHistory/index.tsx',
    'src/pages/Workflow/ProcessDefinition/index.tsx',
    'src/pages/Workflow/JobManagement/index.tsx',
    'src/pages/Notification/index.tsx',
    'src/pages/FeatureToggle/index.tsx',
    'src/pages/File/FileList/index.tsx',
];

filesToFix.forEach(file => {
    let content = fs.readFileSync(file, 'utf8');

    // Check if it imports Search from lucide-react
    if (content.match(/import\s+\{[^}]*\bSearch\b[^}]*\}\s+from\s+['\"]lucide-react['\"]/)) {
        // We will change the import to Search as SearchIcon
        content = content.replace(/(import\s+\{[^}]*\b)(Search)(\b[^}]*\}\s+from\s+['\"]lucide-react['\"])/, '$1Search as SearchIcon$3');

        // Replace usages of the icon
        content = content.replace(/<Search\s+size=\{16\}/g, '<SearchIcon size={16}');
        content = content.replace(/<Search\s*\/>/g, '<SearchIcon />');
        content = content.replace(/icon=\{<Search/g, 'icon={<SearchIcon');
        content = content.replace(/icon=\{Search\}/g, 'icon={SearchIcon}');

        fs.writeFileSync(file, content, 'utf8');
        console.log('Fixed Search conflict in', file);
    }
});
