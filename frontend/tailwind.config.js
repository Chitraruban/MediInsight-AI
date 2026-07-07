/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#0B5FA5',
          light: '#237cbd',
          dark: '#08487e',
          lightest: '#eef6fc'
        },
      },
    },
  },
  plugins: [],
}
