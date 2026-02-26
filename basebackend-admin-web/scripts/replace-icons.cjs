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

const iconMap = {
    DashboardOutlined: 'LayoutDashboard',
    SettingOutlined: 'Settings',
    MonitorOutlined: 'Monitor',
    UserOutlined: 'User',
    TeamOutlined: 'Users',
    BookOutlined: 'Book',
    ApartmentOutlined: 'Network',
    SafetyOutlined: 'Shield',
    LogoutOutlined: 'LogOut',
    MenuFoldOutlined: 'PanelLeftClose',
    MenuUnfoldOutlined: 'PanelLeftOpen',
    UserAddOutlined: 'UserPlus',
    PlusOutlined: 'Plus',
    ReloadOutlined: 'RefreshCw',
    SearchOutlined: 'Search',
    DeleteOutlined: 'Trash2',
    EditOutlined: 'Edit',
    EnvironmentOutlined: 'MapPin',
    ExclamationCircleOutlined: 'AlertCircle',
    ExperimentOutlined: 'FlaskConical',
    EyeOutlined: 'Eye',
    FileAddOutlined: 'FilePlus',
    FileExcelOutlined: 'FileSpreadsheet',
    FileImageOutlined: 'Image',
    FileOutlined: 'File',
    FilePdfOutlined: 'FileText',
    FileTextOutlined: 'FileText',
    FileWordOutlined: 'FileText',
    FileZipOutlined: 'FileArchive',
    FolderOpenOutlined: 'FolderOpen',
    FolderOutlined: 'Folder',
    FormOutlined: 'FormInput',
    FullscreenExitOutlined: 'Minimize',
    FullscreenOutlined: 'Maximize',
    GlobalOutlined: 'Globe',
    HistoryOutlined: 'History',
    InboxOutlined: 'Inbox',
    KeyOutlined: 'Key',
    LaptopOutlined: 'Laptop',
    LayoutOutlined: 'Layout',
    LineChartOutlined: 'LineChart',
    LinkOutlined: 'Link',
    LockOutlined: 'Lock',
    PauseCircleOutlined: 'PauseCircle',
    PlayCircleOutlined: 'PlayCircle',
    PoweroffOutlined: 'Power',
    RedoOutlined: 'Redo',
    RollbackOutlined: 'Undo2',
    SaveOutlined: 'Save',
    SendOutlined: 'Send',
    ShareAltOutlined: 'Share2',
    ShoppingCartOutlined: 'ShoppingCart',
    SnippetsOutlined: 'Clipboard',
    SyncOutlined: 'RefreshCw',
    ThunderboltOutlined: 'Zap',
    UndoOutlined: 'Undo',
    UploadOutlined: 'Upload',
    WarningOutlined: 'TriangleAlert',
    ZoomInOutlined: 'ZoomIn',
    ZoomOutOutlined: 'ZoomOut',
    DownOutlined: 'ChevronDown',
    ArrowLeftOutlined: 'ArrowLeft',
    CheckOutlined: 'Check',
    CheckCircleOutlined: 'CheckCircle2',
    CheckSquareOutlined: 'CheckSquare',
    ClearOutlined: 'X',
    ClockCircleOutlined: 'Clock',
    CloseCircleOutlined: 'XCircle',
    CloudDownloadOutlined: 'CloudDownload',
    CloudServerOutlined: 'Server',
    CloudUploadOutlined: 'CloudUpload',
    CopyOutlined: 'Copy',
    DatabaseOutlined: 'Database',
    DesktopOutlined: 'Monitor',
    DollarOutlined: 'DollarSign',
    DownloadOutlined: 'Download',
    AppstoreOutlined: 'LayoutGrid',
    ApiOutlined: 'Webhook',
    BarsOutlined: 'Menu',
    BellOutlined: 'Bell',
    BulbOutlined: 'Lightbulb',
    SafetyCertificateOutlined: 'ShieldCheck',
    NodeIndexOutlined: 'GitCommit',
    SwapOutlined: 'ArrowLeftRight'
};

const files = findFiles('src', ['.ts', '.tsx']);

files.forEach(file => {
    let content = fs.readFileSync(file, 'utf8');
    let originalContent = content;
    let hasChanges = false;

    // Find all @ant-design/icons imports
    // We use a regex that captures the imported items, handling multiline
    const importRegex = /import\s+\{([^}]+)\}\s+from\s+['\"]@ant-design\/icons['\"]/g;

    content = content.replace(importRegex, (match, importsStr) => {
        hasChanges = true;
        const imports = importsStr.split(',').map(s => s.trim()).filter(Boolean);
        const lucideImports = new Set();

        imports.forEach(i => {
            // It might be like: ArrowLeftOutlined as BackIcon
            let mapped = iconMap[i] || i.replace(/Outlined$/, '');
            if (iconMap[i] === undefined) {
                console.warn(`Missing explicit mapping for ${i} in ${file}. Using ${mapped}`);
            }
            lucideImports.add(mapped);
        });

        return \`import { \${Array.from(lucideImports).join(', ')} } from 'lucide-react'\`;
  });

  if (hasChanges) {
    // Now replace usages in code
    for (const [antdIcon, lucideIcon] of Object.entries(iconMap)) {
      // Replace component usages: <UserOutlined ... /> -> <User size={16} ... />
      // And icon={UserOutlined} -> icon={<User size={16} />} 
      // But wait! React expects a component for icon={} sometimes, or an element.
      // Ant D icons are used both as JSX <UserOutlined /> and as component references `icon: UserOutlined`
      
      const componentUsageRegex = new RegExp(\`<\${antdIcon}\\\\b([^>]*)/?>\`, 'g');
      content = content.replace(componentUsageRegex, \`<\${lucideIcon} size={16}$1/>\`);
      
      // We also need to replace the identifier itself if passed as a prop, e.g. icon={<DashboardOutlined />} handled above.
      // What about icon={DashboardOutlined}? In Procomponents, usually they use strings or ReactNode. 
      // Just replacing the identifier name
      const identifierRegex = new RegExp(\`\\\\b\${antdIcon}\\\\b\`, 'g');
      content = content.replace(identifierRegex, lucideIcon);
    }
    
    // For anything ending in Outlined that wasn't matched
    let match;
    const regex = /<([A-Z][a-zA-Z0-9]*)Outlined\b([^>]*)\/?>/g;
    content = content.replace(regex, (m, name, attrs) => {
        return \`<\${name} size={16}\${attrs}/>\`;
    });

    content = content.replace(/\b([A-Z][a-zA-Z0-9]*)Outlined\b/g, '$1');

    fs.writeFileSync(file, content, 'utf8');
    console.log(`Updated ${ file } `);
  }
});
