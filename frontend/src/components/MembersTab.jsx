import { useCallback, useEffect, useState } from 'react';
import { Edit3, Plus, ShieldAlert, ShieldCheck, Trash2, UserCheck, Users, X } from 'lucide-react';
import { api } from '../api/client';

const EMPTY_MEMBER = {
  name: '',
  email: '',
  phone: '',
  status: 'ACTIVE',
};

export default function MembersTab({ auth, notify, onDataChanged }) {
  const [members, setMembers] = useState([]);
  const [myProfile, setMyProfile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_MEMBER);
  const [submitting, setSubmitting] = useState(false);

  const isStaff = auth && (auth.role === 'ADMIN' || auth.role === 'LIBRARIAN');

  const loadData = useCallback(async () => {
    if (!auth) return;
    setLoading(true);
    try {
      if (isStaff) {
        const res = await api.getMembers();
        setMembers(res.data || []);
      } else if (auth.memberId) {
        const res = await api.getMemberById(auth.memberId);
        setMyProfile(res.data);
      }
    } catch (err) {
      notify(err.message, 'error', err.correlationId);
    } finally {
      setLoading(false);
    }
  }, [auth, isStaff, notify]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleSave = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    const payload = {
      name: form.name.trim(),
      email: form.email.trim(),
      phone: form.phone.trim(),
      status: form.status,
    };
    try {
      const res =
        editing === 'NEW'
          ? await api.createMember(payload)
          : await api.updateMember(editing.id, payload);
      notify(
        editing === 'NEW' ? 'Yeni üye oluşturuldu.' : 'Üye bilgileri güncellendi.',
        'success',
        res.correlationId
      );
      setEditing(null);
      loadData();
      onDataChanged();
    } catch (err) {
      notify(err.message, 'error', err.correlationId);
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleStatus = async (member) => {
    const nextStatus = member.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    try {
      const res = await api.updateMemberStatus(member.id, nextStatus);
      notify(`${member.name} durumu ${nextStatus} olarak güncellendi.`, 'success', res.correlationId);
      loadData();
      onDataChanged();
    } catch (err) {
      notify(err.message, 'error', err.correlationId);
    }
  };

  const handleDelete = async (member) => {
    if (!window.confirm(`"${member.name}" üyesini silmek istediğinize emin misiniz?`)) return;
    try {
      const res = await api.deleteMember(member.id);
      notify('Üye silindi.', 'success', res.correlationId);
      loadData();
      onDataChanged();
    } catch (err) {
      notify(err.message, 'error', err.correlationId);
    }
  };

  if (!auth) {
    return (
      <div className="card">
        <div className="empty-state">
          <Users size={32} />
          <h4>Üye bilgileri için giriş yapın</h4>
          <p>Yetkili hesapla tüm üyeleri yönetebilir, üye hesabıyla kendi profilinizi görebilirsiniz.</p>
        </div>
      </div>
    );
  }

  if (!isStaff) {
    return (
      <div className="card">
        <div className="card-header">
          <div>
            <h3>
              <UserCheck size={17} /> Üye Profilim
            </h3>
            <p>MEMBER rolü yalnızca kendi üye kaydını görüntüleyebilir</p>
          </div>
          {myProfile && (
            <span
              className={`badge ${myProfile.status === 'ACTIVE' ? 'badge-emerald' : 'badge-rose'}`}
            >
              {myProfile.status}
            </span>
          )}
        </div>
        <div className="card-body">
          {loading ? (
            <div className="empty-state">Profil yükleniyor...</div>
          ) : !myProfile ? (
            <div className="empty-state">
              <h4>Üye kaydı bulunamadı</h4>
              <p>
                Member #{auth.memberId} kaydı henüz veritabanında oluşturulmamış olabilir. Admin hesabıyla
                ekleyebilirsiniz.
              </p>
            </div>
          ) : (
            <div className="profile-card">
              <div className="profile-item">
                <span>Ad Soyad</span>
                <strong>{myProfile.name}</strong>
              </div>
              <div className="profile-item">
                <span>E-posta</span>
                <strong>{myProfile.email}</strong>
              </div>
              <div className="profile-item">
                <span>Telefon</span>
                <strong>{myProfile.phone}</strong>
              </div>
              <div className="profile-item">
                <span>Üyelik Tarihi</span>
                <strong>{myProfile.membershipDate || '—'}</strong>
              </div>
            </div>
          )}
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="card">
        <div className="card-header">
          <div>
            <h3>
              <Users size={17} /> Üye Yönetimi
            </h3>
            <p>Toplam {members.length} üye • Üyelik durumu ACTIVE / SUSPENDED olarak değiştirilebilir</p>
          </div>
          <button
            className="btn btn-primary"
            onClick={() => {
              setEditing('NEW');
              setForm(EMPTY_MEMBER);
            }}
          >
            <Plus size={16} /> Yeni Üye
          </button>
        </div>

        <div className="table-wrap">
          {loading ? (
            <div className="empty-state">Üyeler yükleniyor...</div>
          ) : members.length === 0 ? (
            <div className="empty-state">
              <Users size={30} />
              <h4>Kayıtlı üye yok</h4>
              <p>İlk üyeyi ekleyerek ödünç alma akışını başlatabilirsiniz.</p>
            </div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Ad Soyad</th>
                  <th>İletişim</th>
                  <th>Kayıt Tarihi</th>
                  <th>Durum</th>
                  <th>İşlemler</th>
                </tr>
              </thead>
              <tbody>
                {members.map((m) => (
                  <tr key={m.id}>
                    <td className="mono">#{m.id}</td>
                    <td>
                      <strong>{m.name}</strong>
                    </td>
                    <td>
                      <div>{m.email}</div>
                      <div style={{ fontSize: 12.5, color: '#64748b' }}>{m.phone}</div>
                    </td>
                    <td>{m.membershipDate || '—'}</td>
                    <td>
                      <span
                        className={`badge ${m.status === 'ACTIVE' ? 'badge-emerald' : 'badge-rose'}`}
                      >
                        {m.status}
                      </span>
                    </td>
                    <td>
                      <div className="actions-cell">
                        <button
                          className={`btn btn-sm ${m.status === 'ACTIVE' ? 'btn-warning' : 'btn-success'}`}
                          onClick={() => handleToggleStatus(m)}
                        >
                          {m.status === 'ACTIVE' ? (
                            <>
                              <ShieldAlert size={14} /> Askıya Al
                            </>
                          ) : (
                            <>
                              <ShieldCheck size={14} /> Aktifleştir
                            </>
                          )}
                        </button>
                        <button
                          className="btn btn-secondary btn-sm"
                          onClick={() => {
                            setEditing(m);
                            setForm({
                              name: m.name || '',
                              email: m.email || '',
                              phone: m.phone || '',
                              status: m.status || 'ACTIVE',
                            });
                          }}
                        >
                          <Edit3 size={14} />
                        </button>
                        <button className="btn btn-danger btn-sm" onClick={() => handleDelete(m)}>
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {editing && (
        <div className="modal-backdrop" onClick={() => setEditing(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editing === 'NEW' ? 'Yeni Üye Ekle' : `Üyeyi Düzenle (#${editing.id})`}</h3>
              <button className="btn btn-secondary btn-sm" onClick={() => setEditing(null)}>
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleSave}>
              <div className="modal-body">
                <div className="field">
                  <label>Ad Soyad *</label>
                  <input
                    className="input"
                    required
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                  />
                </div>
                <div className="field">
                  <label>E-posta *</label>
                  <input
                    className="input"
                    type="email"
                    required
                    value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                  />
                </div>
                <div className="form-grid">
                  <div className="field">
                    <label>Telefon *</label>
                    <input
                      className="input"
                      required
                      placeholder="0555 000 0000"
                      value={form.phone}
                      onChange={(e) => setForm({ ...form, phone: e.target.value })}
                    />
                  </div>
                  <div className="field">
                    <label>Durum</label>
                    <select
                      className="select"
                      value={form.status}
                      onChange={(e) => setForm({ ...form, status: e.target.value })}
                    >
                      <option value="ACTIVE">ACTIVE</option>
                      <option value="SUSPENDED">SUSPENDED</option>
                    </select>
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setEditing(null)}>
                  İptal
                </button>
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Kaydediliyor...' : 'Kaydet'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
