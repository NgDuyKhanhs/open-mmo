# OpenMMO - Client

Modern portfolio web application built with Vue 3, Vite, and TypeScript for OpenMMO platform.

## ✨ Features

- ⚡ Fast development with Vite
- 🎨 Beautiful UI with TailwindCSS + Responsive Design
- 🗂️ Type-safe with TypeScript (strict mode)
- 📦 State management with Pinia
- 🛣️ Advanced routing with Vue Router v5
- 🎭 Reusable composables for animations and interactions
- 🏗️ Clean architecture with config/stores/composables pattern
- 🔍 Code quality with ESLint, Prettier, Oxlint

## 📋 Requirements

- **Node.js**: v20.19.0 or v22.12.0+
- **npm**: v10+ (comes with Node.js)

Check your versions:
```bash
node --version
npm --version
```

## 🚀 Quick Start

### 1. Clone the repository
```bash
git clone https://github.com/NgDuyKhanhs/open-mmo.git
cd FEOMMO
```

### 2. Install dependencies
```bash
npm install
```

### 3. Start development server
```bash
npm run dev
```

The app will be available at: **http://localhost:5174**

### 4. Build for production
```bash
npm run build
```

Production files will be in the `dist/` folder.

## 📚 Available Commands

| Command | Description |
|---------|-------------|
| `npm run dev` | Start development server |
| `npm run build` | Build for production |
| `npm run preview` | Preview production build locally |
| `npm run type-check` | Run TypeScript type checking |
| `npm run lint` | Run ESLint and Oxlint |
| `npm run format` | Format code with Prettier |

## 🏗️ Project Structure

```
src/
├── config/           # Centralized configuration
│   ├── navigation.ts      # Navigation menu config
│   ├── chat-config.ts     # Chat assistant settings
│   └── services-config.ts # Services database
├── stores/           # Pinia state management
│   ├── useUIStore.ts      # UI state (loading, theme, menu)
│   ├── useChatStore.ts    # Chat message history
│   └── useServicesStore.ts # Services data
├── composables/      # Reusable Vue composables
│   ├── usePhoneTilt.ts        # 3D phone tilt animation
│   ├── useScrollReveal.ts     # Scroll-triggered animations
│   └── useCounterAnimation.ts # Number counter animation
├── views/           # Page components (lazy-loaded)
│   ├── IntroductionView.vue
│   ├── ServicesView.vue
│   ├── ContactView.vue
│   ├── LoginView.vue
│   └── NotFoundView.vue
├── components/      # Reusable UI components
├── router/          # Vue Router configuration
├── App.vue          # Root component
└── main.ts          # Application entry point
```

## 🎯 Key Features

### Configuration Management
All hardcoded data is centralized in `src/config/`:
- Navigation items
- Chat bot responses
- Services database (16 services categorized)

### Type-Safe State Management
Pinia stores with full TypeScript support:
- `useUIStore` - UI state (loading, mobile menu, theme)
- `useChatStore` - Chat message history with conversation simulation
- `useServicesStore` - Services with API integration ready

### Reusable Composables
Extract component logic into library-grade composables:
- Mouse tracking with 3D rotation
- Scroll-triggered animations with IntersectionObserver
- Animated counter values with easing functions

### Advanced Routing
- Lazy-loaded route components (code-splitting)
- Route metadata (title, description, requiresAuth)
- Automatic document title updates
- 404 Not Found page

## 🔧 Development Tips

### Add a new service
Edit `src/config/services-config.ts`:
```typescript
const SERVICES_CONFIG = {
  earning: [
    { id: 'new-service', label: 'My Service', icon: 'icon-name' }
  ]
}
```

### Create a new store
Use Pinia setup pattern in `src/stores/`:
```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useMyStore = defineStore('my', () => {
  const state = ref(initialValue)
  
  const action = () => {
    // do something
  }
  
  return { state, action }
})
```

### Create a reusable composable
Add to `src/composables/`:
```typescript
export const useMyComposable = () => {
  onMounted(() => {
    // setup
  })
  
  onUnmounted(() => {
    // cleanup
  })
  
  return { /* expose API */ }
}
```

## 🐛 Environment Setup

For additional configuration (API endpoints, auth keys, etc.), create `.env.local`:
```bash
VITE_API_URL=https://your-api.com
VITE_AUTH_KEY=your-key
```

**Note:** `.env.local` files are NOT tracked in git (added to .gitignore for security).

## 📦 Dependencies

### Core
- **vue** - Vue 3 framework
- **vue-router** - v5 client-side routing
- **pinia** - State management

### Styling
- **tailwindcss** - Utility-first CSS
- **postcss** - CSS transformation pipeline
- **autoprefixer** - Browser compatibility

### Build & Development
- **vite** - Next-generation frontend tooling
- **typescript** - Type safety
- **eslint** - Code quality
- **prettier** - Code formatting
- **oxlint** - Fast linter

## 🔗 Links

- **Repository**: https://github.com/NgDuyKhanhs/open-mmo
- **Vue 3 Docs**: https://vuejs.org/
- **Vite Docs**: https://vitejs.dev/
- **Vue Router Docs**: https://router.vuejs.org/
- **Pinia Docs**: https://pinia.vuejs.org/
- **TailwindCSS Docs**: https://tailwindcss.com/

## 📝 License

This project is part of the OpenMMO platform.

## 🤝 Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -m "feat: add new feature"`
3. Push to remote: `git push origin feature/your-feature`
4. Open a Pull Request on GitHub

## ⚠️ Troubleshooting

### "npm: command not found"
Install Node.js from https://nodejs.org/ (v20.19.0 or v22.12.0+)

### Port 5174 already in use
The dev server will automatically try the next available port, or specify:
```bash
npm run dev -- --port 3000
```

### TypeScript errors in IDE
Run type checking:
```bash
npm run type-check
```

### Build fails
Clear cache and rebuild:
```bash
rm -rf dist node_modules package-lock.json
npm install
npm run build
```

## 📧 Support

For issues or questions, please open an issue on GitHub.
