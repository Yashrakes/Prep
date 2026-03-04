# Complete BFF (Backend-for-Frontend) Pattern Implementation

## Project Structure

```
my-secure-app/
├── backend/
│   ├── src/
│   │   ├── config/
│   │   │   └── oauth.config.js
│   │   ├── middleware/
│   │   │   ├── auth.middleware.js
│   │   │   ├── errorHandler.middleware.js
│   │   │   └── cors.middleware.js
│   │   ├── routes/
│   │   │   ├── auth.routes.js
│   │   │   └── api.routes.js
│   │   ├── services/
│   │   │   ├── oauth.service.js
│   │   │   ├── token.service.js
│   │   │   └── user.service.js
│   │   ├── utils/
│   │   │   ├── logger.js
│   │   │   └── validators.js
│   │   └── app.js
│   ├── .env
│   ├── .env.example
│   ├── package.json
│   └── server.js
│
├── frontend/
│   ├── src/
│   │   ├── hooks/
│   │   │   └── useAuth.js
│   │   ├── services/
│   │   │   ├── api.service.js
│   │   │   └── auth.service.js
│   │   ├── context/
│   │   │   └── AuthContext.js
│   │   ├── pages/
│   │   │   ├── Login.jsx
│   │   │   ├── Callback.jsx
│   │   │   ├── Dashboard.jsx
│   │   │   └── Profile.jsx
│   │   ├── components/
│   │   │   ├── ProtectedRoute.jsx
│   │   │   └── LoadingSpinner.jsx
│   │   ├── App.jsx
│   │   └── index.jsx
│   ├── .env
│   ├── package.json
│   └── vite.config.js
│
└── docker-compose.yml
```

---

## Backend Implementation

### 1. Backend Setup & Dependencies

```bash
# Initialize backend
mkdir backend && cd backend
npm init -y

# Install dependencies
npm install express dotenv axios cookie-parser cors helmet jsonwebtoken
npm install --save-dev nodemon

# For production
npm install pm2 -g
```

### 2. .env Configuration

**File: `backend/.env`**

```env
# Server
NODE_ENV=development
PORT=5000
BACKEND_URL=http://localhost:5000

# Frontend URL
FRONTEND_URL=http://localhost:3000

# OAuth Provider (Google)
OAUTH_PROVIDER=google
GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=YOUR_GOOGLE_CLIENT_SECRET
GOOGLE_REDIRECT_URI=http://localhost:5000/api/auth/callback

# Session & Tokens
SESSION_SECRET=your-random-session-secret-key-min-32-chars
ACCESS_TOKEN_SECRET=your-random-access-token-secret-min-32-chars
REFRESH_TOKEN_SECRET=your-random-refresh-token-secret-min-32-chars

# Token Lifetimes
ACCESS_TOKEN_EXPIRES_IN=300        # 5 minutes
REFRESH_TOKEN_EXPIRES_IN=604800    # 7 days

# Security
ALLOWED_ORIGINS=http://localhost:3000,https://myapp.com
ENABLE_HTTPS=false
```

### 3. Configuration File

**File: `backend/src/config/oauth.config.js`**

```javascript
require('dotenv').config();

const oauthConfig = {
  google: {
    clientId: process.env.GOOGLE_CLIENT_ID,
    clientSecret: process.env.GOOGLE_CLIENT_SECRET,
    redirectUri: process.env.GOOGLE_REDIRECT_URI,
    authorizationEndpoint: 'https://accounts.google.com/o/oauth2/v2/auth',
    tokenEndpoint: 'https://oauth2.googleapis.com/token',
    userInfoEndpoint: 'https://www.googleapis.com/oauth2/v2/userinfo',
    scopes: ['openid', 'email', 'profile']
  }
};

const tokenConfig = {
  accessTokenExpiresIn: parseInt(process.env.ACCESS_TOKEN_EXPIRES_IN) || 300,
  refreshTokenExpiresIn: parseInt(process.env.REFRESH_TOKEN_EXPIRES_IN) || 604800
};

const cookieConfig = {
  httpOnly: true,
  secure: process.env.NODE_ENV === 'production',
  sameSite: 'strict',
  domain: process.env.NODE_ENV === 'production' ? '.myapp.com' : undefined,
  path: '/',
  maxAge: tokenConfig.refreshTokenExpiresIn * 1000
};

module.exports = {
  oauthConfig,
  tokenConfig,
  cookieConfig,
  sessionSecret: process.env.SESSION_SECRET,
  accessTokenSecret: process.env.ACCESS_TOKEN_SECRET,
  refreshTokenSecret: process.env.REFRESH_TOKEN_SECRET,
  frontendUrl: process.env.FRONTEND_URL,
  backendUrl: process.env.BACKEND_URL,
  allowedOrigins: process.env.ALLOWED_ORIGINS.split(',')
};
```

### 4. Logger Utility

**File: `backend/src/utils/logger.js`**

```javascript
const fs = require('fs');
const path = require('path');

const logsDir = path.join(__dirname, '../../logs');
if (!fs.existsSync(logsDir)) {
  fs.mkdirSync(logsDir);
}

class Logger {
  log(level, message, data = {}) {
    const timestamp = new Date().toISOString();
    const logEntry = {
      timestamp,
      level,
      message,
      ...data
    };

    console.log(`[${timestamp}] ${level}: ${message}`, data);

    // Write to file
    const logFile = path.join(logsDir, `${level.toLowerCase()}.log`);
    fs.appendFileSync(logFile, JSON.stringify(logEntry) + '\n');
  }

  info(message, data) {
    this.log('INFO', message, data);
  }

  error(message, data) {
    this.log('ERROR', message, data);
  }

  warn(message, data) {
    this.log('WARN', message, data);
  }

  debug(message, data) {
    if (process.env.NODE_ENV === 'development') {
      this.log('DEBUG', message, data);
    }
  }
}

module.exports = new Logger();
```

### 5. Token Service

**File: `backend/src/services/token.service.js`**

```javascript
const jwt = require('jsonwebtoken');
const config = require('../config/oauth.config');
const logger = require('../utils/logger');

class TokenService {
  /**
   * Generate access token
   */
  generateAccessToken(userId, userEmail) {
    try {
      const payload = {
        userId,
        email: userEmail,
        type: 'access'
      };

      const token = jwt.sign(payload, config.accessTokenSecret, {
        expiresIn: config.tokenConfig.accessTokenExpiresIn,
        issuer: 'my-secure-app',
        audience: 'api'
      });

      logger.info('Access token generated', { userId });
      return token;
    } catch (error) {
      logger.error('Failed to generate access token', { error: error.message });
      throw error;
    }
  }

  /**
   * Generate refresh token
   */
  generateRefreshToken(userId, userEmail) {
    try {
      const payload = {
        userId,
        email: userEmail,
        type: 'refresh'
      };

      const token = jwt.sign(payload, config.refreshTokenSecret, {
        expiresIn: config.tokenConfig.refreshTokenExpiresIn,
        issuer: 'my-secure-app',
        audience: 'refresh'
      });

      logger.info('Refresh token generated', { userId });
      return token;
    } catch (error) {
      logger.error('Failed to generate refresh token', { error: error.message });
      throw error;
    }
  }

  /**
   * Verify access token
   */
  verifyAccessToken(token) {
    try {
      const decoded = jwt.verify(token, config.accessTokenSecret, {
        issuer: 'my-secure-app',
        audience: 'api'
      });

      if (decoded.type !== 'access') {
        throw new Error('Invalid token type');
      }

      return decoded;
    } catch (error) {
      logger.warn('Access token verification failed', { error: error.message });
      return null;
    }
  }

  /**
   * Verify refresh token
   */
  verifyRefreshToken(token) {
    try {
      const decoded = jwt.verify(token, config.refreshTokenSecret, {
        issuer: 'my-secure-app',
        audience: 'refresh'
      });

      if (decoded.type !== 'refresh') {
        throw new Error('Invalid token type');
      }

      return decoded;
    } catch (error) {
      logger.warn('Refresh token verification failed', { error: error.message });
      return null;
    }
  }

  /**
   * Decode token without verification (for debugging)
   */
  decodeToken(token) {
    return jwt.decode(token);
  }

  /**
   * Check if token is expired
   */
  isTokenExpired(token) {
    try {
      const decoded = jwt.decode(token);
      return decoded.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
}

module.exports = new TokenService();
```

### 6. OAuth Service

**File: `backend/src/services/oauth.service.js`**

```javascript
const axios = require('axios');
const config = require('../config/oauth.config');
const tokenService = require('./token.service');
const logger = require('../utils/logger');

class OAuthService {
  /**
   * Exchange authorization code for tokens
   */
  async exchangeCodeForTokens(code, redirectUri) {
    try {
      logger.info('Exchanging authorization code for tokens');

      const response = await axios.post(
        config.oauthConfig.google.tokenEndpoint,
        {
          client_id: config.oauthConfig.google.clientId,
          client_secret: config.oauthConfig.google.clientSecret,
          code: code,
          grant_type: 'authorization_code',
          redirect_uri: redirectUri
        },
        {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          timeout: 10000
        }
      );

      logger.info('Authorization code exchanged successfully');

      return {
        accessToken: response.data.access_token,
        refreshToken: response.data.refresh_token,
        expiresIn: response.data.expires_in,
        tokenType: response.data.token_type
      };
    } catch (error) {
      logger.error('Failed to exchange authorization code', {
        error: error.message,
        code: error.response?.status
      });
      throw new Error('Failed to exchange authorization code');
    }
  }

  /**
   * Get user info from OAuth provider
   */
  async getUserInfo(oauthAccessToken) {
    try {
      logger.info('Fetching user info from OAuth provider');

      const response = await axios.get(
        config.oauthConfig.google.userInfoEndpoint,
        {
          headers: { Authorization: `Bearer ${oauthAccessToken}` },
          timeout: 10000
        }
      );

      const userInfo = {
        id: response.data.id,
        email: response.data.email,
        name: response.data.name,
        picture: response.data.picture,
        verifiedEmail: response.data.verified_email
      };

      logger.info('User info fetched successfully', { email: userInfo.email });

      return userInfo;
    } catch (error) {
      logger.error('Failed to fetch user info', { error: error.message });
      throw new Error('Failed to fetch user info');
    }
  }

  /**
   * Refresh OAuth access token
   */
  async refreshOAuthAccessToken(refreshToken) {
    try {
      logger.info('Refreshing OAuth access token');

      const response = await axios.post(
        config.oauthConfig.google.tokenEndpoint,
        {
          client_id: config.oauthConfig.google.clientId,
          client_secret: config.oauthConfig.google.clientSecret,
          refresh_token: refreshToken,
          grant_type: 'refresh_token'
        },
        { timeout: 10000 }
      );

      logger.info('OAuth access token refreshed');

      return {
        accessToken: response.data.access_token,
        expiresIn: response.data.expires_in
      };
    } catch (error) {
      logger.error('Failed to refresh OAuth access token', {
        error: error.message
      });
      throw new Error('Failed to refresh OAuth access token');
    }
  }
}

module.exports = new OAuthService();
```

### 7. User Service (Mock Database)

**File: `backend/src/services/user.service.js`**

```javascript
const logger = require('../utils/logger');

/**
 * Mock user database - In production, use a real database (PostgreSQL, MongoDB, etc)
 */
class UserService {
  constructor() {
    this.users = new Map();
  }

  /**
   * Find or create user
   */
  async findOrCreateUser(oauthUserInfo) {
    try {
      let user = this.users.get(oauthUserInfo.id);

      if (!user) {
        user = {
          id: oauthUserInfo.id,
          email: oauthUserInfo.email,
          name: oauthUserInfo.name,
          picture: oauthUserInfo.picture,
          emailVerified: oauthUserInfo.verifiedEmail,
          createdAt: new Date(),
          lastLoginAt: new Date(),
          oauthProvider: 'google'
        };

        this.users.set(oauthUserInfo.id, user);
        logger.info('New user created', { userId: user.id, email: user.email });
      } else {
        user.lastLoginAt = new Date();
        this.users.set(oauthUserInfo.id, user);
        logger.info('User logged in', { userId: user.id });
      }

      return user;
    } catch (error) {
      logger.error('Failed to find or create user', { error: error.message });
      throw error;
    }
  }

  /**
   * Get user by ID
   */
  async getUserById(userId) {
    try {
      const user = this.users.get(userId);
      if (!user) {
        throw new Error('User not found');
      }
      return user;
    } catch (error) {
      logger.error('Failed to get user', { userId, error: error.message });
      throw error;
    }
  }

  /**
   * Update user
   */
  async updateUser(userId, updates) {
    try {
      const user = this.users.get(userId);
      if (!user) {
        throw new Error('User not found');
      }

      const updatedUser = { ...user, ...updates };
      this.users.set(userId, updatedUser);

      logger.info('User updated', { userId });
      return updatedUser;
    } catch (error) {
      logger.error('Failed to update user', { userId, error: error.message });
      throw error;
    }
  }

  /**
   * Delete user
   */
  async deleteUser(userId) {
    try {
      const deleted = this.users.delete(userId);
      if (deleted) {
        logger.info('User deleted', { userId });
      }
      return deleted;
    } catch (error) {
      logger.error('Failed to delete user', { userId, error: error.message });
      throw error;
    }
  }
}

module.exports = new UserService();
```

### 8. CORS Middleware

**File: `backend/src/middleware/cors.middleware.js`**

```javascript
const config = require('../config/oauth.config');

const corsOptions = {
  origin: function (origin, callback) {
    if (!origin || config.allowedOrigins.includes(origin)) {
      callback(null, true);
    } else {
      callback(new Error(`CORS policy violation: ${origin}`));
    }
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  maxAge: 86400 // 24 hours
};

module.exports = corsOptions;
```

### 9. Auth Middleware

**File: `backend/src/middleware/auth.middleware.js`**

```javascript
const tokenService = require('../services/token.service');
const logger = require('../utils/logger');

/**
 * Verify access token from Authorization header
 */
function verifyAccessToken(req, res, next) {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'Missing or invalid authorization header' });
    }

    const token = authHeader.substring(7); // Remove "Bearer " prefix

    const decoded = tokenService.verifyAccessToken(token);
    if (!decoded) {
      return res.status(401).json({ error: 'Invalid or expired token' });
    }

    // Attach user info to request
    req.user = decoded;
    next();
  } catch (error) {
    logger.error('Token verification error', { error: error.message });
    res.status(401).json({ error: 'Unauthorized' });
  }
}

/**
 * Optional auth middleware - doesn't fail if no token
 */
function optionalAuth(req, res, next) {
  try {
    const authHeader = req.headers.authorization;

    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.substring(7);
      const decoded = tokenService.verifyAccessToken(token);
      if (decoded) {
        req.user = decoded;
      }
    }

    next();
  } catch (error) {
    logger.warn('Optional auth check failed', { error: error.message });
    next();
  }
}

module.exports = {
  verifyAccessToken,
  optionalAuth
};
```

### 10. Error Handler Middleware

**File: `backend/src/middleware/errorHandler.middleware.js`**

```javascript
const logger = require('../utils/logger');

function errorHandler(err, req, res, next) {
  logger.error('Unhandled error', {
    message: err.message,
    stack: err.stack,
    path: req.path,
    method: req.method
  });

  // Default error response
  let statusCode = 500;
  let errorMessage = 'Internal server error';

  // Handle specific error types
  if (err.message.includes('CORS')) {
    statusCode = 403;
    errorMessage = err.message;
  } else if (err.message.includes('Unauthorized')) {
    statusCode = 401;
    errorMessage = 'Unauthorized';
  } else if (err.message.includes('Not found')) {
    statusCode = 404;
    errorMessage = 'Not found';
  }

  res.status(statusCode).json({
    error: errorMessage,
    ...(process.env.NODE_ENV === 'development' && { details: err.message })
  });
}

module.exports = errorHandler;
```

### 11. Auth Routes

**File: `backend/src/routes/auth.routes.js`**

```javascript
const express = require('express');
const router = express.Router();
const oauthService = require('../services/oauth.service');
const userService = require('../services/user.service');
const tokenService = require('../services/token.service');
const config = require('../config/oauth.config');
const logger = require('../utils/logger');

/**
 * GET /api/auth/login
 * Redirects user to OAuth provider
 */
router.get('/login', (req, res) => {
  try {
    const { state = '' } = req.query;

    const authUrl = new URL(config.oauthConfig.google.authorizationEndpoint);
    authUrl.searchParams.append('client_id', config.oauthConfig.google.clientId);
    authUrl.searchParams.append('redirect_uri', config.oauthConfig.google.redirectUri);
    authUrl.searchParams.append('response_type', 'code');
    authUrl.searchParams.append('scope', config.oauthConfig.google.scopes.join(' '));
    authUrl.searchParams.append('state', state);
    authUrl.searchParams.append('access_type', 'offline'); // To get refresh token

    logger.info('OAuth login initiated', { state });
    res.json({ redirectUrl: authUrl.toString() });
  } catch (error) {
    logger.error('Login initiation failed', { error: error.message });
    res.status(500).json({ error: 'Failed to initiate login' });
  }
});

/**
 * POST /api/auth/callback
 * Handle OAuth callback - Frontend sends authorization code here
 */
router.post('/callback', async (req, res) => {
  try {
    const { code } = req.body;

    if (!code) {
      return res.status(400).json({ error: 'Missing authorization code' });
    }

    logger.info('OAuth callback received', { codePrefix: code.substring(0, 10) });

    // Step 1: Exchange code for OAuth tokens
    const oauthTokens = await oauthService.exchangeCodeForTokens(
      code,
      config.oauthConfig.google.redirectUri
    );

    // Step 2: Get user info from OAuth provider
    const oauthUserInfo = await oauthService.getUserInfo(oauthTokens.accessToken);

    // Step 3: Find or create user in our system
    const user = await userService.findOrCreateUser(oauthUserInfo);

    // Step 4: Generate our own tokens
    const accessToken = tokenService.generateAccessToken(user.id, user.email);
    const refreshToken = tokenService.generateRefreshToken(user.id, user.email);

    // Step 5: Set refresh token as HttpOnly cookie
    res.cookie('refresh_token', refreshToken, config.cookieConfig);

    logger.info('User authenticated successfully', {
      userId: user.id,
      email: user.email
    });

    // Step 6: Return access token and user info to frontend
    res.json({
      accessToken,
      expiresIn: config.tokenConfig.accessTokenExpiresIn,
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        picture: user.picture
      }
    });
  } catch (error) {
    logger.error('OAuth callback failed', { error: error.message });
    res.status(401).json({ error: 'Authentication failed' });
  }
});

/**
 * POST /api/auth/refresh
 * Refresh access token using refresh token from cookie
 */
router.post('/refresh', async (req, res) => {
  try {
    const refreshToken = req.cookies.refresh_token;

    if (!refreshToken) {
      return res.status(401).json({ error: 'No refresh token found' });
    }

    logger.info('Refresh token request received');

    // Verify refresh token
    const decoded = tokenService.verifyRefreshToken(refreshToken);
    if (!decoded) {
      return res.status(401).json({ error: 'Invalid refresh token' });
    }

    // Get user to ensure they still exist
    const user = await userService.getUserById(decoded.userId);

    // Generate new access token
    const newAccessToken = tokenService.generateAccessToken(user.id, user.email);

    // Optionally rotate refresh token (more secure)
    const newRefreshToken = tokenService.generateRefreshToken(user.id, user.email);
    res.cookie('refresh_token', newRefreshToken, config.cookieConfig);

    logger.info('Access token refreshed', { userId: decoded.userId });

    res.json({
      accessToken: newAccessToken,
      expiresIn: config.tokenConfig.accessTokenExpiresIn
    });
  } catch (error) {
    logger.error('Token refresh failed', { error: error.message });
    res.status(401).json({ error: 'Token refresh failed' });
  }
});

/**
 * POST /api/auth/logout
 * Logout user - clear refresh token cookie
 */
router.post('/logout', (req, res) => {
  try {
    const userId = req.user?.userId;
    logger.info('User logout', { userId });

    // Clear refresh token cookie
    res.clearCookie('refresh_token', {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      path: '/'
    });

    res.json({ success: true, message: 'Logged out successfully' });
  } catch (error) {
    logger.error('Logout failed', { error: error.message });
    res.status(500).json({ error: 'Logout failed' });
  }
});

/**
 * GET /api/auth/me
 * Get current user info (requires valid access token)
 */
router.get('/me', require('../middleware/auth.middleware').verifyAccessToken, async (req, res) => {
  try {
    const user = await userService.getUserById(req.user.userId);

    res.json({
      id: user.id,
      email: user.email,
      name: user.name,
      picture: user.picture,
      emailVerified: user.emailVerified,
      createdAt: user.createdAt,
      lastLoginAt: user.lastLoginAt
    });
  } catch (error) {
    logger.error('Failed to fetch user', { error: error.message });
    res.status(404).json({ error: 'User not found' });
  }
});

module.exports = router;
```

### 12. API Routes

**File: `backend/src/routes/api.routes.js`**

```javascript
const express = require('express');
const router = express.Router();
const { verifyAccessToken } = require('../middleware/auth.middleware');
const userService = require('../services/user.service');
const logger = require('../utils/logger');

/**
 * GET /api/protected-resource
 * Example protected route
 */
router.get('/protected-resource', verifyAccessToken, async (req, res) => {
  try {
    const user = await userService.getUserById(req.user.userId);

    res.json({
      message: 'This is a protected resource',
      user: {
        id: user.id,
        email: user.email,
        name: user.name
      }
    });
  } catch (error) {
    logger.error('Failed to fetch protected resource', { error: error.message });
    res.status(500).json({ error: 'Failed to fetch resource' });
  }
});

/**
 * PUT /api/profile
 * Update user profile
 */
router.put('/profile', verifyAccessToken, async (req, res) => {
  try {
    const { name } = req.body;

    if (!name) {
      return res.status(400).json({ error: 'Name is required' });
    }

    const updatedUser = await userService.updateUser(req.user.userId, { name });

    res.json({
      id: updatedUser.id,
      email: updatedUser.email,
      name: updatedUser.name,
      picture: updatedUser.picture
    });
  } catch (error) {
    logger.error('Failed to update profile', { error: error.message });
    res.status(500).json({ error: 'Failed to update profile' });
  }
});

/**
 * DELETE /api/account
 * Delete user account
 */
router.delete('/account', verifyAccessToken, async (req, res) => {
  try {
    await userService.deleteUser(req.user.userId);

    res.clearCookie('refresh_token');
    res.json({ success: true, message: 'Account deleted' });
  } catch (error) {
    logger.error('Failed to delete account', { error: error.message });
    res.status(500).json({ error: 'Failed to delete account' });
  }
});

module.exports = router;
```

### 13. Express App Setup

**File: `backend/src/app.js`**

```javascript
const express = require('express');
const cookieParser = require('cookie-parser');
const helmet = require('helmet');
const cors = require('cors');

const corsOptions = require('./middleware/cors.middleware');
const errorHandler = require('./middleware/errorHandler.middleware');
const authRoutes = require('./routes/auth.routes');
const apiRoutes = require('./routes/api.routes');
const logger = require('./utils/logger');

const app = express();

// Security middleware
app.use(helmet()); // Sets various HTTP headers

// CORS
app.use(cors(corsOptions));

// Body parsing
app.use(express.json({ limit: '10kb' }));
app.use(express.urlencoded({ limit: '10kb', extended: true }));

// Cookie parsing
app.use(cookieParser());

// Request logging
app.use((req, res, next) => {
  logger.info(`${req.method} ${req.path}`, {
    userAgent: req.headers['user-agent'],
    ip: req.ip
  });
  next();
});

// Routes
app.use('/api/auth', authRoutes);
app.use('/api', apiRoutes);

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({ error: 'Route not found' });
});

// Error handler (must be last)
app.use(errorHandler);

module.exports = app;
```

### 14. Server Entry Point

**File: `backend/server.js`**

```javascript
const app = require('./src/app');
const logger = require('./src/utils/logger');

const PORT = process.env.PORT || 5000;

const server = app.listen(PORT, () => {
  logger.info(`BFF server running on http://localhost:${PORT}`);
});

// Handle graceful shutdown
process.on('SIGTERM', () => {
  logger.info('SIGTERM received, shutting down gracefully');
  server.close(() => {
    logger.info('Server closed');
    process.exit(0);
  });
});

process.on('unhandledRejection', (reason, promise) => {
  logger.error('Unhandled Rejection at:', { promise, reason });
  process.exit(1);
});
```

### 15. Backend Package.json

**File: `backend/package.json`**

```json
{
  "name": "bff-backend",
  "version": "1.0.0",
  "description": "Backend-for-Frontend OAuth 2.0 Implementation",
  "main": "server.js",
  "scripts": {
    "start": "node server.js",
    "dev": "nodemon server.js",
    "test": "jest --detectOpenHandles"
  },
  "keywords": ["oauth", "bff", "backend-for-frontend"],
  "author": "",
  "license": "MIT",
  "dependencies": {
    "axios": "^1.6.0",
    "cookie-parser": "^1.4.6",
    "cors": "^2.8.5",
    "dotenv": "^16.3.1",
    "express": "^4.18.2",
    "helmet": "^7.1.0",
    "jsonwebtoken": "^9.1.2"
  },
  "devDependencies": {
    "nodemon": "^3.0.1"
  },
  "engines": {
    "node": ">=16.0.0"
  }
}
```

---

## Frontend Implementation

### 1. Frontend Setup

```bash
# Initialize frontend with Vite + React
npm create vite@latest frontend -- --template react
cd frontend
npm install

# Install dependencies
npm install axios react-router-dom
```

### 2. Frontend .env

**File: `frontend/.env`**

```env
VITE_API_URL=http://localhost:5000
VITE_GOOGLE_CLIENT_ID=YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com
VITE_FRONTEND_URL=http://localhost:3000
```

### 3. Authentication Context

**File: `frontend/src/context/AuthContext.jsx`**

```javascript
import React, { createContext, useCallback, useEffect, useState } from 'react';

export const AuthContext = createContext({});

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [accessToken, setAccessToken] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshScheduled, setRefreshScheduled] = useState(false);

  const apiUrl = import.meta.env.VITE_API_URL;

  /**
   * Schedule access token refresh before expiration
   */
  const scheduleTokenRefresh = useCallback((expiresIn) => {
    if (refreshScheduled) return;

    // Refresh at 90% of token lifetime
    const refreshTime = expiresIn * 1000 * 0.9;

    const timeoutId = setTimeout(() => {
      refreshAccessToken();
      setRefreshScheduled(false);
    }, refreshTime);

    setRefreshScheduled(true);

    // Cleanup timeout on unmount
    return () => {
      clearTimeout(timeoutId);
      setRefreshScheduled(false);
    };
  }, [refreshScheduled]);

  /**
   * Refresh access token using refresh token from cookie
   */
  const refreshAccessToken = useCallback(async () => {
    try {
      const response = await fetch(`${apiUrl}/api/auth/refresh`, {
        method: 'POST',
        credentials: 'include', // Include refresh_token cookie
        headers: { 'Content-Type': 'application/json' }
      });

      if (!response.ok) {
        throw new Error('Failed to refresh token');
      }

      const data = await response.json();
      setAccessToken(data.accessToken);
      scheduleTokenRefresh(data.expiresIn);
    } catch (err) {
      console.error('Token refresh failed:', err);
      // If refresh fails, log user out
      logout();
    }
  }, [apiUrl, scheduleTokenRefresh]);

  /**
   * Fetch current user info
   */
  const fetchUserInfo = useCallback(async (token) => {
    try {
      const response = await fetch(`${apiUrl}/api/auth/me`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error('Failed to fetch user info');
      }

      const userData = await response.json();
      setUser(userData);
      return userData;
    } catch (err) {
      console.error('Failed to fetch user info:', err);
      throw err;
    }
  }, [apiUrl]);

  /**
   * Handle OAuth callback
   */
  const handleCallback = useCallback(async (code) => {
    try {
      setIsLoading(true);
      setError(null);

      // Send code to backend
      const response = await fetch(`${apiUrl}/api/auth/callback`, {
        method: 'POST',
        credentials: 'include', // Include cookies (for refresh_token)
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code })
      });

      if (!response.ok) {
        throw new Error('Authentication failed');
      }

      const data = await response.json();
      const { accessToken: newAccessToken, expiresIn, user: userData } = data;

      // Store access token in memory
      setAccessToken(newAccessToken);
      setUser(userData);

      // Schedule token refresh
      scheduleTokenRefresh(expiresIn);

      return true;
    } catch (err) {
      setError(err.message);
      console.error('OAuth callback failed:', err);
      return false;
    } finally {
      setIsLoading(false);
    }
  }, [apiUrl, scheduleTokenRefresh]);

  /**
   * Initiate login
   */
  const initiateLogin = useCallback(async () => {
    try {
      setError(null);

      const response = await fetch(`${apiUrl}/api/auth/login`, {
        credentials: 'include'
      });

      if (!response.ok) {
        throw new Error('Failed to initiate login');
      }

      const { redirectUrl } = await response.json();
      window.location.href = redirectUrl;
    } catch (err) {
      setError(err.message);
      console.error('Login initiation failed:', err);
    }
  }, [apiUrl]);

  /**
   * Logout user
   */
  const logout = useCallback(async () => {
    try {
      // Call logout endpoint
      await fetch(`${apiUrl}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Authorization': `Bearer ${accessToken}` }
      });

      // Clear local state
      setAccessToken(null);
      setUser(null);
      setError(null);
      setRefreshScheduled(false);
    } catch (err) {
      console.error('Logout failed:', err);
      // Still clear state even if logout request fails
      setAccessToken(null);
      setUser(null);
    }
  }, [apiUrl, accessToken]);

  /**
   * Check if user is authenticated
   */
  const isAuthenticated = !!accessToken && !!user;

  const value = {
    user,
    accessToken,
    isAuthenticated,
    isLoading,
    error,
    initiateLogin,
    handleCallback,
    logout,
    refreshAccessToken,
    fetchUserInfo,
    apiUrl
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
```

### 4. useAuth Hook

**File: `frontend/src/hooks/useAuth.js`**

```javascript
import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }

  return context;
}
```

### 5. API Service

**File: `frontend/src/services/api.service.js`**

```javascript
/**
 * API Service - Makes requests to BFF backend
 * Automatically includes access token and handles 401 responses
 */
class ApiService {
  constructor() {
    this.baseUrl = import.meta.env.VITE_API_URL;
  }

  /**
   * Make HTTP request with access token
   */
  async request(endpoint, options = {}) {
    const { getAccessToken, refreshToken } = options;

    const token = getAccessToken?.();

    const headers = {
      'Content-Type': 'application/json',
      ...options.headers
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    let response = await fetch(`${this.baseUrl}${endpoint}`, {
      credentials: 'include', // Include refresh_token cookie
      ...options,
      headers
    });

    // If token expired, refresh and retry
    if (response.status === 401 && refreshToken) {
      await refreshToken();
      const newToken = getAccessToken?.();

      if (newToken) {
        headers['Authorization'] = `Bearer ${newToken}`;
        response = await fetch(`${this.baseUrl}${endpoint}`, {
          credentials: 'include',
          ...options,
          headers
        });
      }
    }

    if (!response.ok) {
      const error = await response.json().catch(() => ({ error: 'Unknown error' }));
      throw new Error(error.error || `HTTP ${response.status}`);
    }

    return response.json();
  }

  /**
   * GET request
   */
  get(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'GET' });
  }

  /**
   * POST request
   */
  post(endpoint, body, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'POST',
      body: JSON.stringify(body)
    });
  }

  /**
   * PUT request
   */
  put(endpoint, body, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(body)
    });
  }

  /**
   * DELETE request
   */
  delete(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'DELETE' });
  }
}

export const apiService = new ApiService();
```

### 6. Protected Route Component

**File: `frontend/src/components/ProtectedRoute.jsx`**

```javascript
import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import LoadingSpinner from './LoadingSpinner';

export default function ProtectedRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return children;
}
```

### 7. Loading Spinner Component

**File: `frontend/src/components/LoadingSpinner.jsx`**

```javascript
import React from 'react';

export default function LoadingSpinner() {
  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      height: '100vh',
      backgroundColor: '#f5f5f5'
    }}>
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '20px'
      }}>
        <div style={{
          width: '40px',
          height: '40px',
          border: '4px solid #ddd',
          borderTop: '4px solid #3498db',
          borderRadius: '50%',
          animation: 'spin 1s linear infinite'
        }}></div>
        <p>Loading...</p>
      </div>

      <style>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}
```

### 8. Login Page

**File: `frontend/src/pages/Login.jsx`**

```javascript
import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';

export default function Login() {
  const { initiateLogin, error, isLoading } = useAuth();
  const [loginError, setLoginError] = useState(null);

  const handleLogin = async () => {
    try {
      setLoginError(null);
      await initiateLogin();
    } catch (err) {
      setLoginError(err.message);
    }
  };

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
      backgroundColor: '#f5f5f5'
    }}>
      <div style={{
        backgroundColor: 'white',
        padding: '40px',
        borderRadius: '8px',
        boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
        textAlign: 'center',
        maxWidth: '400px',
        width: '100%'
      }}>
        <h1>Secure Login</h1>
        <p style={{ color: '#666', marginBottom: '30px' }}>
          Sign in with your Google account securely
        </p>

        {(error || loginError) && (
          <div style={{
            backgroundColor: '#fee',
            color: '#c33',
            padding: '12px',
            borderRadius: '4px',
            marginBottom: '20px'
          }}>
            {error || loginError}
          </div>
        )}

        <button
          onClick={handleLogin}
          disabled={isLoading}
          style={{
            width: '100%',
            padding: '12px',
            backgroundColor: '#3498db',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            fontSize: '16px',
            cursor: isLoading ? 'not-allowed' : 'pointer',
            opacity: isLoading ? 0.6 : 1,
            fontWeight: 'bold'
          }}
        >
          {isLoading ? 'Redirecting...' : 'Sign in with Google'}
        </button>

        <div style={{
          marginTop: '30px',
          paddingTop: '20px',
          borderTop: '1px solid #eee',
          fontSize: '14px',
          color: '#666'
        }}>
          <h3>How it works:</h3>
          <ol style={{ textAlign: 'left' }}>
            <li>Click "Sign in with Google"</li>
            <li>Authenticate with your Google account</li>
            <li>Grant permission for email and profile</li>
            <li>You'll be logged in securely</li>
          </ol>
        </div>
      </div>
    </div>
  );
}
```

### 9. OAuth Callback Handler

**File: `frontend/src/pages/Callback.jsx`**

```javascript
import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import LoadingSpinner from '../components/LoadingSpinner';

export default function Callback() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { handleCallback, isLoading, error } = useAuth();

  useEffect(() => {
    const handleAuth = async () => {
      const code = searchParams.get('code');
      const errorParam = searchParams.get('error');

      if (errorParam) {
        console.error('OAuth error:', errorParam);
        navigate('/login');
        return;
      }

      if (!code) {
        console.error('No authorization code received');
        navigate('/login');
        return;
      }

      // Send code to backend
      const success = await handleCallback(code);

      if (success) {
        // Redirect to dashboard after successful authentication
        navigate('/dashboard');
      } else {
        // Stay on this page if authentication failed
        navigate('/login');
      }
    };

    handleAuth();
  }, [searchParams, handleCallback, navigate]);

  if (isLoading) {
    return <LoadingSpinner />;
  }

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh'
    }}>
      <div style={{
        backgroundColor: '#fee',
        color: '#c33',
        padding: '20px',
        borderRadius: '4px',
        maxWidth: '400px'
      }}>
        <h2>Authentication Error</h2>
        <p>{error || 'Failed to authenticate. Please try again.'}</p>
        <button
          onClick={() => navigate('/login')}
          style={{
            padding: '10px 20px',
            backgroundColor: '#3498db',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Back to Login
        </button>
      </div>
    </div>
  );
}
```

### 10. Dashboard Page

**File: `frontend/src/pages/Dashboard.jsx`**

```javascript
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { apiService } from '../services/api.service';

export default function Dashboard() {
  const navigate = useNavigate();
  const { user, logout, accessToken, refreshAccessToken, apiUrl } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchProtectedData();
  }, [accessToken]);

  const fetchProtectedData = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await apiService.get('/api/protected-resource', {
        getAccessToken: () => accessToken,
        refreshToken: refreshAccessToken
      });

      setData(response);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div style={{
      maxWidth: '1200px',
      margin: '0 auto',
      padding: '40px 20px'
    }}>
      {/* Header */}
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: '40px',
        borderBottom: '2px solid #eee',
        paddingBottom: '20px'
      }}>
        <h1>Dashboard</h1>
        <button
          onClick={handleLogout}
          style={{
            padding: '10px 20px',
            backgroundColor: '#e74c3c',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontWeight: 'bold'
          }}
        >
          Logout
        </button>
      </div>

      {/* User Info Card */}
      <div style={{
        backgroundColor: 'white',
        padding: '30px',
        borderRadius: '8px',
        boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
        marginBottom: '30px'
      }}>
        <h2>Welcome, {user?.name || 'User'}!</h2>
        
        {user?.picture && (
          <img
            src={user.picture}
            alt="Profile"
            style={{
              width: '80px',
              height: '80px',
              borderRadius: '50%',
              marginTop: '20px',
              marginBottom: '20px'
            }}
          />
        )}

        <div style={{ lineHeight: '1.8' }}>
          <p><strong>Email:</strong> {user?.email}</p>
          <p><strong>User ID:</strong> {user?.id}</p>
          <p><strong>Verified:</strong> {user?.emailVerified ? '✓ Yes' : '✗ No'}</p>
          <p><strong>Account Created:</strong> {new Date(user?.createdAt).toLocaleDateString()}</p>
          <p><strong>Last Login:</strong> {new Date(user?.lastLoginAt).toLocaleDateString()}</p>
        </div>
      </div>

      {/* Protected Data Card */}
      <div style={{
        backgroundColor: 'white',
        padding: '30px',
        borderRadius: '8px',
        boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
        marginBottom: '30px'
      }}>
        <h3>Protected Resource</h3>

        <button
          onClick={fetchProtectedData}
          disabled={loading}
          style={{
            padding: '10px 20px',
            backgroundColor: '#27ae60',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: loading ? 'not-allowed' : 'pointer',
            marginBottom: '20px',
            opacity: loading ? 0.6 : 1
          }}
        >
          {loading ? 'Loading...' : 'Fetch Protected Data'}
        </button>

        {error && (
          <div style={{
            backgroundColor: '#fee',
            color: '#c33',
            padding: '12px',
            borderRadius: '4px'
          }}>
            Error: {error}
          </div>
        )}

        {data && (
          <pre style={{
            backgroundColor: '#f5f5f5',
            padding: '20px',
            borderRadius: '4px',
            overflow: 'auto'
          }}>
            {JSON.stringify(data, null, 2)}
          </pre>
        )}
      </div>

      {/* Security Info */}
      <div style={{
        backgroundColor: '#e3f2fd',
        padding: '20px',
        borderRadius: '8px',
        borderLeft: '4px solid #2196f3'
      }}>
        <h3>Security Features</h3>
        <ul>
          <li>✓ Access token stored in memory (not exposed to XSS)</li>
          <li>✓ Refresh token in HttpOnly secure cookie (not accessible to JavaScript)</li>
          <li>✓ HTTPS-only in production</li>
          <li>✓ CSRF protection via SameSite cookies</li>
          <li>✓ Automatic token refresh before expiration</li>
          <li>✓ Server-side OAuth token exchange</li>
        </ul>
      </div>
    </div>
  );
}
```

### 11. Profile Page

**File: `frontend/src/pages/Profile.jsx`**

```javascript
import React, { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { apiService } from '../services/api.service';

export default function Profile() {
  const { user, accessToken, refreshAccessToken } = useAuth();
  const [name, setName] = useState(user?.name || '');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState(null);

  const handleUpdate = async (e) => {
    e.preventDefault();

    try {
      setLoading(true);
      setMessage(null);

      await apiService.put('/api/profile', { name }, {
        getAccessToken: () => accessToken,
        refreshToken: refreshAccessToken
      });

      setMessage({ type: 'success', text: 'Profile updated successfully!' });
    } catch (error) {
      setMessage({ type: 'error', text: error.message });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      maxWidth: '600px',
      margin: '40px auto',
      padding: '20px'
    }}>
      <h1>Edit Profile</h1>

      {message && (
        <div style={{
          padding: '12px',
          borderRadius: '4px',
          marginBottom: '20px',
          backgroundColor: message.type === 'success' ? '#e8f5e9' : '#fee',
          color: message.type === 'success' ? '#2e7d32' : '#c33'
        }}>
          {message.text}
        </div>
      )}

      <form onSubmit={handleUpdate}>
        <div style={{ marginBottom: '20px' }}>
          <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
            Name
          </label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={{
              width: '100%',
              padding: '10px',
              border: '1px solid #ddd',
              borderRadius: '4px',
              fontSize: '16px',
              boxSizing: 'border-box'
            }}
            required
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          style={{
            padding: '12px 24px',
            backgroundColor: '#3498db',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: loading ? 'not-allowed' : 'pointer',
            opacity: loading ? 0.6 : 1,
            fontWeight: 'bold'
          }}
        >
          {loading ? 'Updating...' : 'Update Profile'}
        </button>
      </form>
    </div>
  );
}
```

### 12. App Setup with Router

**File: `frontend/src/App.jsx`**

```javascript
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Callback from './pages/Callback';
import Dashboard from './pages/Dashboard';
import Profile from './pages/Profile';

export default function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/callback" element={<Callback />} />
          
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
          
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <Profile />
              </ProtectedRoute>
            }
          />
          
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </Router>
  );
}
```

### 13. Frontend Package.json

**File: `frontend/package.json`**

```json
{
  "name": "bff-frontend",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.16.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.37",
    "@types/react-dom": "^18.2.15",
    "@vitejs/plugin-react": "^4.2.0",
    "vite": "^5.0.0"
  }
}
```

---

## Docker Setup

**File: `docker-compose.yml`**

```yaml
version: '3.8'

services:
  backend:
    build:
      context: ./backend
    ports:
      - "5000:5000"
    environment:
      - NODE_ENV=development
      - PORT=5000
      - FRONTEND_URL=http://localhost:3000
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
      - SESSION_SECRET=${SESSION_SECRET:-secret-key-change-in-production}
      - ACCESS_TOKEN_SECRET=${ACCESS_TOKEN_SECRET:-access-secret-key}
      - REFRESH_TOKEN_SECRET=${REFRESH_TOKEN_SECRET:-refresh-secret-key}
    volumes:
      - ./backend/src:/app/src
    command: npm run dev

  frontend:
    build:
      context: ./frontend
    ports:
      - "3000:5173"
    environment:
      - VITE_API_URL=http://localhost:5000
      - VITE_GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
    volumes:
      - ./frontend/src:/app/src
    command: npm run dev

volumes:
  backend_logs:
```

---

## Getting Started

### 1. Setup Google OAuth Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project
3. Enable Google+ API
4. Create OAuth 2.0 credentials (Web application)
5. Add authorized redirect URIs:
   - `http://localhost:5000/api/auth/callback`
   - `http://localhost:3000/callback`
6. Copy `Client ID` and `Client Secret`

### 2. Setup Environment Variables

```bash
# Copy .env.example to .env for both backend and frontend
cp backend/.env.example backend/.env
cp frontend/.env frontend/.env

# Edit and add your Google OAuth credentials
```

### 3. Run Locally

```bash
# Terminal 1 - Backend
cd backend
npm install
npm run dev

# Terminal 2 - Frontend
cd frontend
npm install
npm run dev
```

Visit `http://localhost:3000`

### 4. Run with Docker

```bash
docker-compose up
```

---

## Key Security Features Implemented

✅ **Access Token in Memory** - Not vulnerable to localStorage/sessionStorage theft

✅ **Refresh Token in HttpOnly Cookie** - Not accessible to JavaScript, prevents XSS token theft

✅ **Server-Side OAuth Exchange** - Client secret never exposed to browser

✅ **Token Rotation** - New refresh token issued with each refresh

✅ **Short-lived Access Tokens** - 5-minute lifetime limits exposure window

✅ **Automatic Token Refresh** - Frontend transparently refreshes before expiration

✅ **CORS Protection** - Whitelist of allowed origins

✅ **CSRF Protection** - SameSite=Strict cookies

✅ **Secure Cookies** - HttpOnly, Secure, SameSite flags

✅ **Request Logging** - All auth events logged for monitoring

✅ **Error Handling** - Graceful error handling without exposing internals

---

## Production Deployment Checklist

- [ ] Use HTTPS/TLS certificates
- [ ] Set `ENABLE_HTTPS=true`
- [ ] Use environment variables for secrets
- [ ] Set secure cookie domain
- [ ] Enable HSTS header
- [ ] Use real database (PostgreSQL, MongoDB)
- [ ] Implement rate limiting
- [ ] Add monitoring/alerting
- [ ] Use horizontal scaling
- [ ] Implement refresh token rotation in database
- [ ] Add audit logging
- [ ] Set up CI/CD pipeline
- [ ] Enable security headers
- [ ] Use API gateway/reverse proxy (nginx, AWS ALB)
