# Gemini Web App Frontend

React TypeScript frontend for the Gemini AI web application.

## Features

- React 18 with TypeScript
- Tailwind CSS for styling
- React Router for navigation
- Axios for API communication
- Authentication with Google OAuth and email
- Responsive design

## Development

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

## Project Structure

```
src/
├── components/     # Reusable UI components
├── pages/         # Page components
├── hooks/         # Custom React hooks
├── services/      # API service layer
├── types/         # TypeScript type definitions
├── utils/         # Utility functions
├── App.tsx        # Main app component
└── main.tsx       # App entry point
```

## API Integration

The frontend communicates with the Spring Boot backend API running on port 8080. API calls are proxied through Vite's development server.