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

const getLucideIcon = (antdName) => {
    if (iconMap[antdName]) return iconMap[antdName];
    return antdName.replace(/Outlined$/, '');
}

const files = findFiles('src', ['.ts', '.tsx']);

files.forEach(file => {
    let content = fs.readFileSync(file, 'utf8');
    let originalContent = content;
    let hasChanges = false;

    // Re-run matching just for unreplaced components
    const importRegex = /import\s+\{([^}]+)\}\s+from\s+['\"]lucide-react['\"]/g;
    let match;
    let allLucideImports = new Set();
    while ((match = importRegex.exec(content)) !== null) {
        match[1].split(',').map(s => s.trim()).filter(Boolean).forEach(i => allLucideImports.add(i));
    }

    // Find any remaining "Outlined" occurrences
    const remainingRegex = /\b([A-Z][a-zA-Z0-9]*)Outlined\b/g;
    let remainingMatch;
    while ((remainingMatch = remainingRegex.exec(content)) !== null) {
        if (remainingMatch[1] === undefined) continue;
        hasChanges = true;
        const originalName = remainingMatch[0];
        const lucideIcon = getLucideIcon(originalName);
        allLucideImports.add(lucideIcon);

        // We will blindly replace it as identifier
        content = content.replace(new RegExp(`\\b${originalName}\\b`, 'g'), lucideIcon);
    }

    if (hasChanges) {
        // Rebuild the lucide-react import
        if (allLucideImports.size > 0) {
            content = content.replace(/import\s+\{([^}]+)\}\s+from\s+['\"]lucide-react['\"];?\n?/g, '');
            content = `import { ${Array.from(allLucideImports).join(', ')} } from 'lucide-react';\n` + content;
        }
        fs.writeFileSync(file, content, 'utf8');
        console.log(`Updated ${file}`);
    }
});
