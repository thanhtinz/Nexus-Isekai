/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Nexus Isekai brand palette
        brand: {
          50:  '#f0edff',
          100: '#e0d9ff',
          200: '#c5b8ff',
          300: '#a48dff',
          400: '#8a62ff',
          500: '#6c3ef3',
          600: '#5a2ed9',
          700: '#4820b8',
          800: '#361595',
          900: '#250f72',
        },
        surface: {
          50:  '#f4f4ff',
          100: '#e8e8ff',
          200: '#d0d0f0',
          800: '#1a1a35',
          850: '#131328',
          900: '#0d0d24',
          950: '#080818',
        },
        gold: {
          300: '#ffe082',
          400: '#ffd54f',
          500: '#f0c050',
          600: '#d4a820',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        display: ['Rajdhani', 'sans-serif'],
      },
      backgroundImage: {
        'hero-pattern': "url('/assets/hero-bg.webp')",
        'card-gradient': 'linear-gradient(135deg, rgba(108,62,243,0.15), rgba(13,13,36,0.8))',
      },
      animation: {
        'shimmer': 'shimmer 2s infinite',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'float': 'float 3s ease-in-out infinite',
      },
      keyframes: {
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-6px)' },
        },
      },
    },
  },
  plugins: [],
}
