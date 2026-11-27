import {
  Activity,
  AlertTriangle,
  BookOpen,
  BookMarked,
  CheckCircle2,
  Copy,
  ShieldCheck,
  Users,
} from 'lucide-react';

export default function DashboardTab({ auth, stats, traces, onNavigate, onCopyCid, onOpenLogin }) {
  const isStaff = auth && (auth.role === 'ADMIN' || auth.role === 'LIBRARIAN');

  return (
    <>
      <div className="stats-grid">
        <div className="stat-card">
          <div>
            <div className="stat-label">Katalogdaki Kitaplar</div>
            <div className="stat-value">{stats.totalBooks}</div>
            <div className="stat-sub">Müsait stok: {stats.availableCopies} kopya</div>
          </div>
          <div className="stat-icon indigo">
            <BookOpen size={20} />
          </div>
        </div>

        <div className="stat-card">
          <div>
            <div className="stat-label">Aktif Ödünçler</div>
            <div className="stat-value">{auth ? stats.activeBorrowings : '—'}</div>
            <div className="stat-sub">
              {auth ? 'BORROWED + OVERDUE' : 'Görmek için giriş yapın'}
            </div>
          </div>
          <div className="stat-icon emerald">
            <BookMarked size={20} />
          </div>
        </div>

        <div className="stat-card">
          <div>
            <div className="stat-label">Gecikmiş Kayıtlar</div>
            <div className="stat-value">{auth ? stats.overdueBorrowings : '—'}</div>
            <div className="stat-sub">Günlük ceza: 5 ₺</div>
          </div>
          <div className="stat-icon rose">
            <AlertTriangle size={20} />
          </div>
        </div>

        <div className="stat-card">
          <div>
            <div className="stat-label">{isStaff ? 'Kayıtlı Üyeler' : 'Aktif Rol'}</div>
            <div className="stat-value">{isStaff ? stats.totalMembers : auth ? auth.role : 'MİSAFİR'}</div>
            <div className="stat-sub">
              {isStaff
                ? `Aktif üye: ${stats.activeMembers}`
                : auth
                  ? `Member ID: ${auth.memberId ?? '-'}`
                  : 'Katalog herkese açık'}
            </div>
          </div>
          <div className="stat-icon amber">
            <Users size={20} />
          </div>
        </div>
      </div>

      <div className="dashboard-grid">
        <div className="card">
          <div className="card-header">
            <div>
              <h3>
                <Activity size={17} /> Canlı İstek Takibi (X-Correlation-Id)
              </h3>
              <p>Gateway → Borrowing → Book / Member hattından dönen son istek izleri</p>
            </div>
            <span className="badge badge-indigo">Faz 5 Aktif</span>
          </div>
          <div className="card-body">
            {traces.length === 0 ? (
              <div className="empty-state">
                <Activity size={28} />
                <h4>Henüz istek izi yok</h4>
                <p>Katalog veya ödünç sekmelerini kullandıkça canlı takip ID&apos;leri burada görünür.</p>
              </div>
            ) : (
              <div className="trace-list">
                {traces.slice(0, 6).map((t) => (
                  <div key={t.id} className="trace-row">
                    <div className="trace-left">
                      <span
                        className={`badge ${
                          t.status >= 400
                            ? 'badge-rose'
                            : t.status >= 200 && t.status < 300
                              ? 'badge-emerald'
                              : 'badge-slate'
                        }`}
                      >
                        {t.method} {t.status}
                      </span>
                      <span className="trace-path" title={t.path}>
                        {t.path}
                      </span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span className="mono" style={{ color: '#64748b' }}>
                        {t.durationMs}ms
                      </span>
                      <button
                        type="button"
                        className="cid-pill"
                        onClick={() => onCopyCid(t.correlationId)}
                        title={`Kopyala: ${t.correlationId}`}
                      >
                        {t.correlationId.slice(0, 8)}
                        <Copy size={12} />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <div>
              <h3>
                <ShieldCheck size={17} /> Oturum ve Hızlı Erişim
              </h3>
              <p>JWT rol tabanlı mikroservis erişim özeti</p>
            </div>
          </div>
          <div className="card-body" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div className="profile-item">
              <span>Mevcut Durum</span>
              <strong style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <CheckCircle2 size={16} color="#059669" />
                {auth ? `${auth.username} (${auth.role})` : 'Misafir (yalnızca katalog okuma)'}
              </strong>
            </div>

            <div style={{ fontSize: 13, color: '#475569', lineHeight: 1.6 }}>
              {isStaff ? (
                <>
                  <strong>Yetkili moddasınız:</strong> Kitap ekleme/güncelleme/silme, tüm üyeleri yönetme ve
                  herhangi bir üye adına ödünç alma/iade/uzatma işlemlerini yapabilirsiniz.
                </>
              ) : auth ? (
                <>
                  <strong>Üye modundasınız (Member #{auth.memberId}):</strong> Yalnızca kendi adınıza ödünç
                  alabilir, kendi kayıtlarınızı uzatıp iade edebilir ve profilinizi görüntüleyebilirsiniz.
                </>
              ) : (
                <>
                  <strong>Misafir modundasınız:</strong> Kitap kataloğunu arayabilirsiniz. Ödünç alma, üye
                  yönetimi veya kitap ekleme için demo hesaplardan biriyle giriş yapın.
                </>
              )}
            </div>

            <div className="toolbar">
              <button className="btn btn-primary" onClick={() => onNavigate('books')}>
                <BookOpen size={16} /> Kitaplara Git
              </button>
              {auth ? (
                <button className="btn btn-secondary" onClick={() => onNavigate('borrowings')}>
                  <BookMarked size={16} /> Ödünç İşlemleri
                </button>
              ) : (
                <button className="btn btn-secondary" onClick={onOpenLogin}>
                  Demo Giriş Yap
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
