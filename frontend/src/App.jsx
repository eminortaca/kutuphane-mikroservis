import { useCallback, useEffect, useState } from 'react';
import {
  Activity,
  BookMarked,
  BookOpen,
  Copy,
  LayoutDashboard,
  Library,
  LogIn,
  LogOut,
  Users,
} from 'lucide-react';
import { api, getStoredAuth, saveAuth, setTraceListener } from './api/client';
import BooksTab from './components/BooksTab';
import BorrowingsTab from './components/BorrowingsTab';
import DashboardTab from './components/DashboardTab';
import LoginModal from './components/LoginModal';
import MembersTab from './components/MembersTab';

const TAB_META = {
  dashboard: {
    title: 'Genel Bakış',
    subtitle: 'Mikroservis kütüphane durumu ve canlı X-Correlation-Id takibi',
  },
  books: {
    title: 'Kitap Kataloğu',
    subtitle: 'Book-Service (8081) üzerinden arama, sayfalama ve stok yönetimi',
  },
  borrowings: {
    title: 'Ödünç İşlemleri',
    subtitle: 'Borrowing-Service (8082) üzerinden ödünç alma, süre uzatma ve iade',
  },
  members: {
    title: 'Üyeler',
    subtitle: 'Member-Service (8083) üzerinden üye kaydı ve durum yönetimi',
  },
};

export default function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [auth, setAuth] = useState(() => getStoredAuth());
  const [loginOpen, setLoginOpen] = useState(false);
  const [traces, setTraces] = useState([]);
  const [toasts, setToasts] = useState([]);
  const [members, setMembers] = useState([]);
  const [stats, setStats] = useState({
    totalBooks: 0,
    availableCopies: 0,
    activeBorrowings: 0,
    overdueBorrowings: 0,
    totalMembers: 0,
    activeMembers: 0,
  });

  const notify = useCallback((message, type = 'info', correlationId = null) => {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 6)}`;
    setToasts((prev) => [...prev.slice(-3), { id, message, type, correlationId }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4500);
  }, []);

  useEffect(() => {
    setTraceListener((entry) => {
      setTraces((prev) => [entry, ...prev].slice(0, 15));
    });
    return () => setTraceListener(null);
  }, []);

  const refreshOverview = useCallback(async () => {
    try {
      const bookRes = await api.getBooks({ page: 0, size: 100 });
      const availableCopies = bookRes.items.reduce((sum, b) => sum + (b.stock || 0), 0);

      let activeBorrowings = 0;
      let overdueBorrowings = 0;
      let totalMembers = 0;
      let activeMembers = 0;

      if (auth) {
        const isStaff = auth.role === 'ADMIN' || auth.role === 'LIBRARIAN';
        const borrowRes = await api.getBorrowings({
          memberId: isStaff ? '' : auth.memberId,
        });
        const list = borrowRes.data || [];
        activeBorrowings = list.filter((b) => b.status === 'BORROWED' || b.status === 'OVERDUE').length;
        overdueBorrowings = list.filter((b) => b.status === 'OVERDUE').length;

        if (isStaff) {
          const memberRes = await api.getMembers();
          const mList = memberRes.data || [];
          setMembers(mList);
          totalMembers = mList.length;
          activeMembers = mList.filter((m) => m.status === 'ACTIVE').length;
        } else {
          setMembers([]);
        }
      } else {
        setMembers([]);
      }

      setStats({
        totalBooks: bookRes.totalCount,
        availableCopies,
        activeBorrowings,
        overdueBorrowings,
        totalMembers,
        activeMembers,
      });
    } catch (err) {
      if (err.status === 401) {
        saveAuth(null);
        setAuth(null);
        notify('Oturum süresi doldu, lütfen tekrar giriş yapın.', 'error', err.correlationId);
      }
    }
  }, [auth, notify]);

  useEffect(() => {
    refreshOverview();
  }, [refreshOverview]);

  const handleLoginSuccess = (loginData) => {
    saveAuth(loginData);
    setAuth(loginData);
  };

  const handleLogout = () => {
    saveAuth(null);
    setAuth(null);
    setMembers([]);
    notify('Oturum kapatıldı.', 'info');
  };

  const handleCopyCid = async (cid) => {
    if (!cid || cid === '-') return;
    try {
      await navigator.clipboard.writeText(cid);
      notify(`Correlation ID kopyalandı: ${cid}`, 'success');
    } catch {
      notify(`Correlation ID: ${cid}`, 'info');
    }
  };

  const lastCid = traces[0]?.correlationId;
  const meta = TAB_META[activeTab];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">
            <Library size={22} />
          </div>
          <div>
            <h1>Kütüphane MS</h1>
            <span>Spring Cloud • Gateway :8080</span>
          </div>
        </div>

        <nav className="nav-list">
          <button
            className={`nav-item ${activeTab === 'dashboard' ? 'active' : ''}`}
            onClick={() => setActiveTab('dashboard')}
          >
            <LayoutDashboard size={18} /> Genel Bakış
          </button>
          <button
            className={`nav-item ${activeTab === 'books' ? 'active' : ''}`}
            onClick={() => setActiveTab('books')}
          >
            <BookOpen size={18} /> Kitap Kataloğu
          </button>
          <button
            className={`nav-item ${activeTab === 'borrowings' ? 'active' : ''}`}
            onClick={() => setActiveTab('borrowings')}
          >
            <BookMarked size={18} /> Ödünç İşlemleri
          </button>
          <button
            className={`nav-item ${activeTab === 'members' ? 'active' : ''}`}
            onClick={() => setActiveTab('members')}
          >
            <Users size={18} /> {auth?.role === 'MEMBER' ? 'Profilim' : 'Üyeler'}
          </button>
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-user">
            <div className="avatar">{auth ? auth.username.slice(0, 2).toUpperCase() : 'MS'}</div>
            <div className="sidebar-user-meta">
              <strong>{auth ? auth.username : 'Misafir Kullanıcı'}</strong>
              <span>{auth ? `${auth.role}${auth.memberId ? ` • #${auth.memberId}` : ''}` : 'Giriş yapılmadı'}</span>
            </div>
          </div>

          {auth ? (
            <button className="btn btn-ghost-dark btn-sm" onClick={handleLogout}>
              <LogOut size={15} /> Çıkış Yap
            </button>
          ) : (
            <button className="btn btn-primary btn-sm" style={{ width: '100%' }} onClick={() => setLoginOpen(true)}>
              <LogIn size={15} /> Giriş Yap (JWT)
            </button>
          )}
        </div>
      </aside>

      <div className="main-area">
        <header className="topbar">
          <div className="topbar-title">
            <h2>{meta.title}</h2>
            <p>{meta.subtitle}</p>
          </div>

          <div className="topbar-actions">
            {lastCid && lastCid !== '-' && (
              <button
                type="button"
                className="cid-pill"
                onClick={() => handleCopyCid(lastCid)}
                title={`Son X-Correlation-Id: ${lastCid}`}
              >
                <Activity size={13} />
                cid: {lastCid.slice(0, 8)}
                <Copy size={12} />
              </button>
            )}

            {auth ? (
              <span className="badge badge-indigo">
                {auth.username} • {auth.role}
              </span>
            ) : (
              <button className="btn btn-primary btn-sm" onClick={() => setLoginOpen(true)}>
                <LogIn size={15} /> Giriş Yap
              </button>
            )}
          </div>
        </header>

        <main className="content">
          {activeTab === 'dashboard' && (
            <DashboardTab
              auth={auth}
              stats={stats}
              traces={traces}
              onNavigate={setActiveTab}
              onCopyCid={handleCopyCid}
              onOpenLogin={() => setLoginOpen(true)}
            />
          )}
          {activeTab === 'books' && (
            <BooksTab auth={auth} members={members} notify={notify} onDataChanged={refreshOverview} />
          )}
          {activeTab === 'borrowings' && (
            <BorrowingsTab auth={auth} members={members} notify={notify} onDataChanged={refreshOverview} />
          )}
          {activeTab === 'members' && (
            <MembersTab auth={auth} notify={notify} onDataChanged={refreshOverview} />
          )}
        </main>
      </div>

      {loginOpen && (
        <LoginModal
          onClose={() => setLoginOpen(false)}
          onSuccess={handleLoginSuccess}
          notify={notify}
        />
      )}

      <div className="toast-stack">
        {toasts.map((t) => (
          <div key={t.id} className={`toast ${t.type}`}>
            <div>{t.message}</div>
            {t.correlationId && t.correlationId !== '-' && (
              <div className="toast-cid">X-Correlation-Id: {t.correlationId}</div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
