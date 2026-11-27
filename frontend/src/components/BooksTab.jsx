import { useCallback, useEffect, useState } from 'react';
import {
  BookOpen,
  BookPlus,
  ChevronLeft,
  ChevronRight,
  Edit3,
  Plus,
  Search,
  Trash2,
  X,
} from 'lucide-react';
import { api } from '../api/client';

const EMPTY_BOOK = {
  title: '',
  author: '',
  isbn: '',
  category: '',
  publishYear: '',
  stock: 1,
  totalCopies: 1,
};

export default function BooksTab({ auth, members, notify, onDataChanged }) {
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState({ title: '', author: '', category: '' });
  const [page, setPage] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const [editingBook, setEditingBook] = useState(null);
  const [bookForm, setBookForm] = useState(EMPTY_BOOK);
  const [borrowTarget, setBorrowTarget] = useState(null);
  const [borrowMemberId, setBorrowMemberId] = useState('');
  const [borrowDays, setBorrowDays] = useState('14');
  const [submitting, setSubmitting] = useState(false);

  const isStaff = auth && (auth.role === 'ADMIN' || auth.role === 'LIBRARIAN');

  const loadBooks = useCallback(
    async (targetPage = page, targetFilters = filters) => {
      setLoading(true);
      try {
        const res = await api.getBooks({ ...targetFilters, page: targetPage, size: 8 });
        setBooks(res.items);
        setTotalCount(res.totalCount);
        setTotalPages(res.totalPages);
      } catch (err) {
        notify(err.message, 'error', err.correlationId);
      } finally {
        setLoading(false);
      }
    },
    [filters, notify, page]
  );

  useEffect(() => {
    loadBooks(page, filters);
  }, [page]);

  const handleSearch = (e) => {
    e.preventDefault();
    setPage(0);
    loadBooks(0, filters);
  };

  const openCreateModal = () => {
    setEditingBook('NEW');
    setBookForm(EMPTY_BOOK);
  };

  const openEditModal = (book) => {
    setEditingBook(book);
    setBookForm({
      title: book.title || '',
      author: book.author || '',
      isbn: book.isbn || '',
      category: book.category || '',
      publishYear: book.publishYear ?? '',
      stock: book.stock ?? 0,
      totalCopies: book.totalCopies ?? book.stock ?? 0,
    });
  };

  const handleSaveBook = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    const payload = {
      title: bookForm.title.trim(),
      author: bookForm.author.trim(),
      isbn: bookForm.isbn.trim() || null,
      category: bookForm.category.trim() || null,
      publishYear: bookForm.publishYear === '' ? null : Number(bookForm.publishYear),
      stock: Number(bookForm.stock),
      totalCopies: bookForm.totalCopies === '' ? Number(bookForm.stock) : Number(bookForm.totalCopies),
    };

    try {
      const res =
        editingBook === 'NEW'
          ? await api.createBook(payload)
          : await api.updateBook(editingBook.id, payload);
      notify(
        editingBook === 'NEW' ? 'Yeni kitap eklendi.' : 'Kitap bilgileri güncellendi.',
        'success',
        res.correlationId
      );
      setEditingBook(null);
      loadBooks(page, filters);
      onDataChanged();
    } catch (err) {
      notify(err.message, 'error', err.correlationId);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteBook = async (book) => {
    if (!window.confirm(`"${book.title}" kitabını silmek istediğinize emin misiniz?`)) return;
    try {
      const res = await api.deleteBook(book.id);
      notify('Kitap silindi.', 'success', res.correlationId);
      loadBooks(page, filters);
      onDataChanged();
    } catch (err) {
      notify(err.message, 'error', err.correlationId);
    }
  };

  const openBorrowModal = (book) => {
    setBorrowTarget(book);
    setBorrowMemberId(auth?.role === 'MEMBER' ? String(auth.memberId || '') : members[0]?.id ? String(members[0].id) : '1');
    setBorrowDays('14');
  };

  const handleBorrowSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const res = await api.borrowBook({
        bookId: borrowTarget.id,
        memberId: Number(borrowMemberId),
        durationDays: borrowDays ? Number(borrowDays) : undefined,
      });
      notify(`"${borrowTarget.title}" ödünç alındı.`, 'success', res.correlationId);
      setBorrowTarget(null);
      loadBooks(page, filters);
      onDataChanged();
    } catch (err) {
      notify(err.message, 'error', err.correlationId);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <div className="card">
        <div className="card-header">
          <div>
            <h3>
              <BookOpen size={17} /> Kitap Kataloğu
            </h3>
            <p>Toplam {totalCount} kitap bulundu • Arama, sayfalama ve anlık stok durumu</p>
          </div>
          {isStaff && (
            <button className="btn btn-primary" onClick={openCreateModal}>
              <Plus size={16} /> Yeni Kitap
            </button>
          )}
        </div>

        <div className="card-body" style={{ borderBottom: '1px solid #e2e8f0' }}>
          <form className="toolbar" onSubmit={handleSearch}>
            <input
              className="input"
              placeholder="Kitap adı ara..."
              value={filters.title}
              onChange={(e) => setFilters({ ...filters, title: e.target.value })}
            />
            <input
              className="input"
              placeholder="Yazar ara..."
              value={filters.author}
              onChange={(e) => setFilters({ ...filters, author: e.target.value })}
            />
            <input
              className="input"
              placeholder="Kategori..."
              value={filters.category}
              onChange={(e) => setFilters({ ...filters, category: e.target.value })}
            />
            <button className="btn btn-primary" type="submit">
              <Search size={15} /> Filtrele
            </button>
            {(filters.title || filters.author || filters.category) && (
              <button
                className="btn btn-secondary"
                type="button"
                onClick={() => {
                  const empty = { title: '', author: '', category: '' };
                  setFilters(empty);
                  setPage(0);
                  loadBooks(0, empty);
                }}
              >
                Temizle
              </button>
            )}
          </form>
        </div>

        <div className="table-wrap">
          {loading ? (
            <div className="empty-state">Kitaplar yükleniyor...</div>
          ) : books.length === 0 ? (
            <div className="empty-state">
              <BookOpen size={30} />
              <h4>Kayıtlı kitap bulunamadı</h4>
              <p>Filtreleri temizleyin veya yetkili hesapla yeni kitap ekleyin.</p>
            </div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Kitap &amp; Yazar</th>
                  <th>Kategori / Yıl</th>
                  <th>ISBN</th>
                  <th>Stok</th>
                  <th>İşlemler</th>
                </tr>
              </thead>
              <tbody>
                {books.map((book) => (
                  <tr key={book.id}>
                    <td className="mono">#{book.id}</td>
                    <td>
                      <strong>{book.title}</strong>
                      <div style={{ fontSize: 12.5, color: '#64748b' }}>{book.author}</div>
                    </td>
                    <td>
                      {book.category ? <span className="badge badge-slate">{book.category}</span> : '—'}
                      {book.publishYear ? (
                        <span style={{ marginLeft: 8, color: '#64748b', fontSize: 12.5 }}>
                          {book.publishYear}
                        </span>
                      ) : null}
                    </td>
                    <td className="mono">{book.isbn || '—'}</td>
                    <td>
                      <span className={`badge ${book.stock > 0 ? 'badge-emerald' : 'badge-rose'}`}>
                        {book.stock} / {book.totalCopies}
                      </span>
                    </td>
                    <td>
                      <div className="actions-cell">
                        {auth && (
                          <button
                            className="btn btn-success btn-sm"
                            disabled={book.stock <= 0}
                            onClick={() => openBorrowModal(book)}
                          >
                            <BookPlus size={14} /> Ödünç Al
                          </button>
                        )}
                        {isStaff && (
                          <>
                            <button
                              className="btn btn-secondary btn-sm"
                              onClick={() => openEditModal(book)}
                            >
                              <Edit3 size={14} />
                            </button>
                            <button
                              className="btn btn-danger btn-sm"
                              onClick={() => handleDeleteBook(book)}
                            >
                              <Trash2 size={14} />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="pagination">
          <span>
            Sayfa {page + 1} / {totalPages} ({totalCount} kayıt)
          </span>
          <div className="toolbar">
            <button
              className="btn btn-secondary btn-sm"
              disabled={page <= 0}
              onClick={() => setPage((p) => p - 1)}
            >
              <ChevronLeft size={15} /> Önceki
            </button>
            <button
              className="btn btn-secondary btn-sm"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Sonraki <ChevronRight size={15} />
            </button>
          </div>
        </div>
      </div>

      {/* Kitap Ekle / Düzenle Modal */}
      {editingBook && (
        <div className="modal-backdrop" onClick={() => setEditingBook(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editingBook === 'NEW' ? 'Yeni Kitap Ekle' : `Kitabı Düzenle (#${editingBook.id})`}</h3>
              <button className="btn btn-secondary btn-sm" onClick={() => setEditingBook(null)}>
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleSaveBook}>
              <div className="modal-body">
                <div className="field">
                  <label>Kitap Adı *</label>
                  <input
                    className="input"
                    required
                    value={bookForm.title}
                    onChange={(e) => setBookForm({ ...bookForm, title: e.target.value })}
                  />
                </div>
                <div className="field">
                  <label>Yazar *</label>
                  <input
                    className="input"
                    required
                    value={bookForm.author}
                    onChange={(e) => setBookForm({ ...bookForm, author: e.target.value })}
                  />
                </div>
                <div className="form-grid">
                  <div className="field">
                    <label>Kategori</label>
                    <input
                      className="input"
                      placeholder="Roman, Bilim..."
                      value={bookForm.category}
                      onChange={(e) => setBookForm({ ...bookForm, category: e.target.value })}
                    />
                  </div>
                  <div className="field">
                    <label>Yayın Yılı</label>
                    <input
                      className="input"
                      type="number"
                      min="1000"
                      max="2100"
                      value={bookForm.publishYear}
                      onChange={(e) => setBookForm({ ...bookForm, publishYear: e.target.value })}
                    />
                  </div>
                </div>
                <div className="field">
                  <label>ISBN</label>
                  <input
                    className="input"
                    placeholder="978-975-..."
                    value={bookForm.isbn}
                    onChange={(e) => setBookForm({ ...bookForm, isbn: e.target.value })}
                  />
                </div>
                <div className="form-grid">
                  <div className="field">
                    <label>Mevcut Stok *</label>
                    <input
                      className="input"
                      type="number"
                      min="0"
                      required
                      value={bookForm.stock}
                      onChange={(e) => setBookForm({ ...bookForm, stock: e.target.value })}
                    />
                  </div>
                  <div className="field">
                    <label>Toplam Kopya</label>
                    <input
                      className="input"
                      type="number"
                      min="0"
                      value={bookForm.totalCopies}
                      onChange={(e) => setBookForm({ ...bookForm, totalCopies: e.target.value })}
                    />
                  </div>
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setEditingBook(null)}>
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

      {/* Ödünç Al Modal */}
      {borrowTarget && (
        <div className="modal-backdrop" onClick={() => setBorrowTarget(null)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>Kitap Ödünç Al</h3>
              <button className="btn btn-secondary btn-sm" onClick={() => setBorrowTarget(null)}>
                <X size={16} />
              </button>
            </div>
            <form onSubmit={handleBorrowSubmit}>
              <div className="modal-body">
                <div className="profile-item">
                  <span>Seçilen Kitap</span>
                  <strong>
                    {borrowTarget.title} — {borrowTarget.author}
                  </strong>
                </div>

                <div className="field">
                  <label>Üye ID *</label>
                  {isStaff && members.length > 0 ? (
                    <select
                      className="select"
                      value={borrowMemberId}
                      onChange={(e) => setBorrowMemberId(e.target.value)}
                      required
                    >
                      {members.map((m) => (
                        <option key={m.id} value={m.id}>
                          #{m.id} — {m.name} ({m.status})
                        </option>
                      ))}
                    </select>
                  ) : (
                    <input
                      className="input"
                      type="number"
                      min="1"
                      required
                      disabled={auth?.role === 'MEMBER'}
                      value={borrowMemberId}
                      onChange={(e) => setBorrowMemberId(e.target.value)}
                    />
                  )}
                </div>

                <div className="field">
                  <label>Ödünç Süresi (Gün)</label>
                  <input
                    className="input"
                    type="number"
                    min="1"
                    max="60"
                    value={borrowDays}
                    onChange={(e) => setBorrowDays(e.target.value)}
                  />
                </div>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary" onClick={() => setBorrowTarget(null)}>
                  Vazgeç
                </button>
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'İşleniyor...' : 'Ödünç İşlemini Onayla'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
