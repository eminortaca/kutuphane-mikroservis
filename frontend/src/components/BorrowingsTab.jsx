import { useCallback, useEffect, useState } from 'react';
import { BookMarked, CalendarPlus, CheckCircle2, RefreshCw } from 'lucide-react';
import { api } from '../api/client';

const STATUS_BADGE = {
  BORROWED: { label: 'Ödünçte', cls: 'badge-indigo' },
  OVERDUE: { label: 'Gecikmiş', cls: 'badge-rose' },
  RETURNED: { label: 'İade Edildi', cls: 'badge-emerald' },
};

export default function BorrowingsTab({ auth, members, notify, onDataChanged }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState('');
  const [memberFilter, setMemberFilter] = useState('');
  const [busyId, setBusyId] = useState(null);

  const isStaff = auth && (auth.role === 'ADMIN' || auth.role === 'LIBRARIAN');

  const loadBorrowings = useCallback(async () => {
    if (!auth) return;
    setLoading(true);
    try {
      const effectiveMemberId = auth.role === 'MEMBER' ? auth.memberId : memberFilter;
      const res = await api.getBorrowings({
        status: statusFilter,
        memberId: effectiveMemberId,
      });
      setItems(res.data || []);
    } catch (err) {
      notify(err.message, 'error', err.correlationId);
    } finally {
      setLoading(false);
    }
  }, [auth, memberFilter, notify, statusFilter]);

  useEffect(() => {
    loadBorrowings();
  }, [loadBorrowings]);

  const handleReturn = async (record) => {
    setBusyId(record.id);
    try {
      const res = await api.returnBook(record.id);
      notify(`"${record.bookTitle}" iade edildi.`, 'success', res.correlationId);
      loadBorrowings();
      onDataChanged();
    } catch (err) {
      notify(err.message, 'error', err.correlationId);
    } finally {
      setBusyId(null);
    }
  };

  const handleRenew = async (record) => {
    setBusyId(record.id);
    try {
      const res = await api.renewBorrowing(record.id);
      notify(`"${record.bookTitle}" süresi uzatıldı.`, 'success', res.correlationId);
      loadBorrowings();
      onDataChanged();
    } catch (err) {
      notify(err.message, 'error', err.correlationId);
    } finally {
      setBusyId(null);
    }
  };

  if (!auth) {
    return (
      <div className="card">
        <div className="empty-state">
          <BookMarked size={32} />
          <h4>Ödünç kayıtları için giriş yapın</h4>
          <p>JWT kimlik doğrulaması sonrası kendi kayıtlarınızı veya tüm kayıtları yönetebilirsiniz.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="card-header">
        <div>
          <h3>
            <BookMarked size={17} /> Ödünç İşlemleri
          </h3>
          <p>
            {isStaff
              ? 'Tüm üyelerin ödünç, süre uzatma ve iade kayıtları'
              : `Üye #${auth.memberId} adına kayıtlı ödünç geçmişi`}
          </p>
        </div>

        <div className="toolbar">
          <select
            className="select"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">Tüm Durumlar</option>
            <option value="BORROWED">Ödünçte (BORROWED)</option>
            <option value="OVERDUE">Gecikmiş (OVERDUE)</option>
            <option value="RETURNED">İade Edildi (RETURNED)</option>
          </select>

          {isStaff && (
            <select
              className="select"
              value={memberFilter}
              onChange={(e) => setMemberFilter(e.target.value)}
            >
              <option value="">Tüm Üyeler</option>
              {members.map((m) => (
                <option key={m.id} value={m.id}>
                  #{m.id} — {m.name}
                </option>
              ))}
            </select>
          )}

          <button className="btn btn-secondary" onClick={loadBorrowings}>
            <RefreshCw size={15} /> Yenile
          </button>
        </div>
      </div>

      <div className="table-wrap">
        {loading ? (
          <div className="empty-state">Ödünç kayıtları yükleniyor...</div>
        ) : items.length === 0 ? (
          <div className="empty-state">
            <BookMarked size={30} />
            <h4>Ödünç kaydı bulunamadı</h4>
            <p>Kitaplar sekmesinden mevcut stoklu bir kitabı hemen ödünç alabilirsiniz.</p>
          </div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Kitap</th>
                <th>Üye</th>
                <th>Tarihler</th>
                <th>Uzatma / Ceza</th>
                <th>Durum</th>
                <th>İşlemler</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => {
                const badge = STATUS_BADGE[item.status] || { label: item.status, cls: 'badge-slate' };
                const isActive = item.status === 'BORROWED' || item.status === 'OVERDUE';
                return (
                  <tr key={item.id}>
                    <td className="mono">#{item.id}</td>
                    <td>
                      <strong>{item.bookTitle}</strong>
                      <div className="mono" style={{ color: '#64748b' }}>
                        Book #{item.bookId}
                      </div>
                    </td>
                    <td>
                      <strong>{item.borrowerName || '—'}</strong>
                      <div className="mono" style={{ color: '#64748b' }}>
                        Member #{item.memberId ?? '-'}
                      </div>
                    </td>
                    <td>
                      <div>Alış: {item.borrowDate || '—'}</div>
                      <div style={{ fontSize: 12.5, color: '#64748b' }}>
                        Son teslim: {item.dueDate || '—'}
                        {item.returnDate ? ` • İade: ${item.returnDate}` : ''}
                      </div>
                    </td>
                    <td>
                      <div>Uzatma: {item.renewCount ?? 0} / 1</div>
                      <div
                        style={{
                          fontSize: 12.5,
                          fontWeight: 700,
                          color: item.lateFee > 0 ? '#e11d48' : '#64748b',
                        }}
                      >
                        Ceza: {(item.lateFee ?? 0).toFixed(2)} ₺
                      </div>
                    </td>
                    <td>
                      <span className={`badge ${badge.cls}`}>{badge.label}</span>
                    </td>
                    <td>
                      {isActive ? (
                        <div className="actions-cell">
                          <button
                            className="btn btn-warning btn-sm"
                            disabled={busyId === item.id || item.status === 'OVERDUE' || (item.renewCount ?? 0) >= 1}
                            onClick={() => handleRenew(item)}
                            title="Süreyi 7 gün uzat (max 1 kez)"
                          >
                            <CalendarPlus size={14} /> Uzat
                          </button>
                          <button
                            className="btn btn-success btn-sm"
                            disabled={busyId === item.id}
                            onClick={() => handleReturn(item)}
                          >
                            <CheckCircle2 size={14} /> İade Et
                          </button>
                        </div>
                      ) : (
                        <span style={{ color: '#94a3b8', fontSize: 12.5 }}>Tamamlandı</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
