import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath } from 'node:url'

export default defineConfig({
  plugins: [
    react(),
    // Tailwind v4 chạy như một plugin của Vite, không cần file
    // tailwind.config.js và không cần postcss.config.js như v3.
    tailwindcss(),
  ],

  resolve: {
    alias: {
      // Không dùng __dirname: Vite 8 đọc file config bằng ESM gốc của
      // Node, ở đó __dirname không tồn tại. Và phải qua fileURLToPath chứ
      // không lấy .pathname trực tiếp — trên Windows .pathname trả về
      // '/D:/TNTT/...' (dư dấu / đầu) nên đường dẫn sẽ sai.
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },

  server: {
    port: 5173,
    // Cho phép mở từ điện thoại trong cùng wifi để thử giao diện thật:
    //   npm run dev  ->  Network: http://192.168.x.x:5173
    // Rất cần vì màn hình điểm danh phải test trên máy thật, không phải
    // chế độ giả lập mobile của trình duyệt.
    host: true,

    proxy: {
      // Mọi request tới /api/... được Vite chuyển tiếp sang backend.
      //
      // Vì sao dùng proxy khi backend ĐÃ bật CORS?
      // Vì với trình duyệt, frontend và API lúc này CÙNG origin
      // (localhost:5173) — nên cookie refresh token hoạt động y hệt
      // production mà không phải vật lộn với SameSite ở môi trường dev.
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },

      // WebSocket của màn hình trực cổng (Sprint 7).
      //
      // `ws: true` là BẮT BUỘC và dễ quên: thiếu nó thì Vite xử lý /ws như
      // một request HTTP thường, không chuyển tiếp bước nâng cấp giao thức,
      // và client nhận về một trang HTML thay vì một kết nối WebSocket.
      //
      // Cần proxy chứ không nối thẳng tới cổng 8080 vì hai lý do: giữ cho
      // frontend và backend cùng origin (giống hệt production sau Nginx), và
      // để mã nguồn không phải hardcode cổng ở bất cứ đâu.
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
})
