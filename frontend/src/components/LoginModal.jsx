import { useState } from 'react';
import { Lock, LogIn, X } from 'lucide-react';
import { api } from '../api/client';

const DEMO_ACCOUNTS = [
  { username: 'admin', password: 'admin123', label: 'ADMIN', desc: 'Tüm yetkiler' },
  { username: 'librarian', password: 'lib123', label: 'LIBRARIAN', desc: 'Kitap, üye, ödünç' },
  { username: 'member1', password: 'member123', label: 'MEMBER #1', desc: 'Sadece kendi kayıtları' },
  { username: 'member2', password: 'member123', label: 'MEMBER #2', desc: 'IDOR testi için 2. üye' },
];

export default function LoginModal({ onClose, onSuccess, notify }) {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await api.login(username, password);
      onSuccess(res.data);
      notify(`Hoş geldiniz, ${res.data.username} (${res.data.role})`, 'success', res.correlationId);
      onClose();
    } catch (err) {
      notify(err.message, 'error', err.correlationId);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Lock size={18} /> JWT Giriş
          </h3>
          <button className="btn btn-secondary btn-sm" onClick={onClose} type="button">
            <X size={16} />
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            <div>
              <div style={{ fontSize: 12, fontWeight: 700, color: '#64748b', marginBottom: 8 }}>
                HIZLI DEMO HESAPLARI
              </div>
              <div className="demo-grid">
                {DEMO_ACCOUNTS.map((acc) => (
                  <button
                    key={acc.username}
                    type="button"
                    className="demo-btn"
                    onClick={() => {
                      setUsername(acc.username);
                      setPassword(acc.password);
                    }}
                  >
                    <strong>
                      {acc.username} <span className="badge badge-indigo">{acc.label}</span>
                    </strong>
                    <span>{acc.desc}</span>
                  </button>
                ))}
              </div>
            </div>

            <div className="field">
              <label>Kullanıcı Adı</label>
              <input
                className="input"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>

            <div className="field">
              <label>Şifre</label>
              <input
                className="input"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Vazgeç
            </button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              <LogIn size={16} />
              {loading ? 'Giriş yapılıyor...' : 'Giriş Yap'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
