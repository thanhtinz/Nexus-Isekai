import { useState } from 'react';

export default function SupportPage() {
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [sent, setSent] = useState(false);

  const faqs = [
    { q: 'Làm sao để nạp diamond?', a: 'Vào game → bấm nút 💎 → chọn gói → thanh toán qua SePay QR.' },
    { q: 'Quên mật khẩu?', a: 'Bấm "Quên mật khẩu" ở màn hình đăng nhập hoặc liên hệ hỗ trợ.' },
    { q: 'Bị mất vật phẩm?', a: 'Gửi ticket hỗ trợ với mô tả chi tiết, chúng tôi sẽ kiểm tra log.' },
    { q: 'Làm sao liên kết Google/Facebook?', a: 'Vào Settings → Tài Khoản → Liên kết tài khoản.' },
    { q: 'Game lag, giật?', a: 'Vào Settings → Graphics → giảm chất lượng hoặc bật Chế độ tiết kiệm pin.' },
  ];

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 to-gray-800 text-white">
      <div className="max-w-3xl mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-center mb-8 text-yellow-400">🎧 Hỗ Trợ</h1>

        <div className="mb-10">
          <h2 className="text-xl font-bold mb-4">❓ Câu hỏi thường gặp</h2>
          <div className="space-y-3">
            {faqs.map((f, i) => (
              <details key={i} className="bg-gray-800 rounded-lg p-4 cursor-pointer">
                <summary className="font-medium">{f.q}</summary>
                <p className="mt-2 text-gray-400 text-sm">{f.a}</p>
              </details>
            ))}
          </div>
        </div>

        <div className="bg-gray-800 rounded-xl p-6">
          <h2 className="text-xl font-bold mb-4">📩 Gửi yêu cầu hỗ trợ</h2>
          {sent ? (
            <div className="text-center py-8">
              <p className="text-green-400 text-lg font-bold">✅ Đã gửi thành công!</p>
              <p className="text-gray-400 mt-2">Chúng tôi sẽ phản hồi trong 24h.</p>
              <button onClick={() => setSent(false)} className="mt-4 text-yellow-400">Gửi thêm</button>
            </div>
          ) : (<>
            <input value={subject} onChange={e => setSubject(e.target.value)} placeholder="Tiêu đề"
              className="w-full bg-gray-700 rounded-lg p-3 mb-3 text-white" />
            <textarea value={message} onChange={e => setMessage(e.target.value)} placeholder="Mô tả chi tiết vấn đề..."
              rows={5} className="w-full bg-gray-700 rounded-lg p-3 mb-3 text-white resize-none" />
            <button onClick={() => { if (subject && message) setSent(true); }}
              className="w-full bg-yellow-500 text-black font-bold py-3 rounded-lg hover:bg-yellow-400 transition">
              Gửi
            </button>
          </>)}
        </div>

        <div className="mt-8 text-center text-gray-500 text-sm">
          <p>Facebook: <a href="#" className="text-yellow-400">fb.com/NexusIsekai</a></p>
          <p>Discord: <a href="#" className="text-yellow-400">discord.gg/NexusIsekai</a></p>
          <p>Email: support@nexusisekai.com</p>
        </div>
      </div>
    </div>
  );
}
