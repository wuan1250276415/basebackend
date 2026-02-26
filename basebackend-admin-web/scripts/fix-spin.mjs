import fs from 'fs';

const files = [
    'src/pages/Workflow/TaskManagement/MyInitiated.tsx',
    'src/pages/Workflow/ProcessInstance/index.tsx',
    'src/pages/Workflow/ProcessInstance/Detail.tsx',
    'src/pages/Workflow/JobManagement/index.tsx',
    'src/pages/Dashboard/components/CoreMetrics.tsx',
    'src/components/Workflow/Timeline.tsx',
    'src/components/Workflow/StatusTags.tsx',
    'src/components/Workflow/Statistics.tsx',
    'src/components/Workflow/ProcessTrackingViewer.tsx',
];

files.forEach(f => {
    let content = fs.readFileSync(f, 'utf8');
    content = content.replace(/<RefreshCw\s+spin\s*\/>/g, '<RefreshCw className="anticon-spin" />');
    content = content.replace(/<RefreshCw\s+spin\s+([^>]+)\/>/g, '<RefreshCw className="anticon-spin" $1/>');
    content = content.replace(/<RefreshCw\s+spin=\{([^}]+)\}\s*\/>/g, '<RefreshCw className={$1 ? "anticon-spin" : ""} />');
    fs.writeFileSync(f, content);
    console.log('Fixed spin in ', f);
});
