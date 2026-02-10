# Frontend Implementation Guide: Dynamic Header & Footer

## Overview

This guide shows how to implement dynamic header and footer components that fetch settings from the backend API and display them dynamically.

## API Endpoint

**GET** `/api/admin/system-settings` (Public - No authentication required)

**Response:**
```json
{
  "success": true,
  "message": "System settings retrieved successfully",
  "data": {
    "logoUrl": "/assets/images/logo.png",
    "logoHeader": "Revenue & Settlement Department",
    "logoSubheader": "Government of Manipur",
    "stateName": "Manipur",
    "footerText": "Revenue & Settlement Department, Government of Manipur",
    "footerCopyright": "© 2024 Government of Manipur. All rights reserved.",
    "footerAddress": "Imphal, Manipur, India",
    "footerEmail": "info@manipur.gov.in",
    "footerPhone": "+91-XXX-XXXXXXX",
    "footerWebsite": "https://manipur.gov.in"
  }
}
```

---

## Angular Implementation

### Step 1: Create Service

**File:** `src/app/services/system-settings.service.ts`

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface SystemSettings {
  id: number;
  logoUrl: string | null;
  logoHeader: string | null;
  logoSubheader: string | null;
  stateName: string | null;
  footerText: string | null;
  footerCopyright: string | null;
  footerAddress: string | null;
  footerEmail: string | null;
  footerPhone: string | null;
  footerWebsite: string | null;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class SystemSettingsService {
  private apiUrl = 'http://localhost:8080/api/admin/system-settings';
  private settingsSubject = new BehaviorSubject<SystemSettings | null>(null);
  public settings$ = this.settingsSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadSettings();
  }

  loadSettings(): void {
    this.http.get<ApiResponse<SystemSettings>>(this.apiUrl).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.settingsSubject.next(response.data);
        }
      },
      error: (error) => {
        console.error('Failed to load system settings:', error);
      }
    });
  }

  getSettings(): Observable<SystemSettings | null> {
    return this.settings$;
  }
}
```

### Step 2: Create Header Component

**File:** `src/app/components/header/header.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { SystemSettingsService, SystemSettings } from '../../services/system-settings.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {
  settings$: Observable<SystemSettings | null>;

  constructor(private settingsService: SystemSettingsService) {
    this.settings$ = this.settingsService.getSettings();
  }

  ngOnInit(): void {}
}
```

**File:** `src/app/components/header/header.component.html`

```html
<header class="app-header">
  <div class="header-container">
    <!-- Logo -->
    <div class="logo-section" *ngIf="settings$ | async as settings">
      <img 
        *ngIf="settings.logoUrl" 
        [src]="settings.logoUrl" 
        alt="Logo" 
        class="logo-image"
        (error)="$event.target.style.display='none'"
      />
    </div>

    <!-- Header Text -->
    <div class="header-text-section" *ngIf="settings$ | async as settings">
      <h1 class="logo-header">{{ settings.logoHeader || 'Revenue & Settlement Department' }}</h1>
      <h2 class="logo-subheader">{{ settings.logoSubheader || 'Government of Manipur' }}</h2>
      <span class="state-name">{{ settings.stateName || 'Manipur' }}</span>
    </div>
  </div>
</header>
```

**File:** `src/app/components/header/header.component.css`

```css
.app-header {
  background-color: #fff;
  border-bottom: 2px solid #e0e0e0;
  padding: 1rem 2rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.header-container {
  display: flex;
  align-items: center;
  gap: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.logo-section {
  flex-shrink: 0;
}

.logo-image {
  max-height: 80px;
  width: auto;
  object-fit: contain;
}

.header-text-section {
  flex: 1;
}

.logo-header {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: #1a1a1a;
}

.logo-subheader {
  margin: 0.25rem 0;
  font-size: 1.1rem;
  font-weight: 400;
  color: #666;
}

.state-name {
  display: block;
  font-size: 0.9rem;
  color: #888;
  margin-top: 0.25rem;
}
```

### Step 3: Create Footer Component

**File:** `src/app/components/footer/footer.component.ts`

```typescript
import { Component, OnInit } from '@angular/core';
import { SystemSettingsService, SystemSettings } from '../../services/system-settings.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.css']
})
export class FooterComponent implements OnInit {
  settings$: Observable<SystemSettings | null>;

  constructor(private settingsService: SystemSettingsService) {
    this.settings$ = this.settingsService.getSettings();
  }

  ngOnInit(): void {}
}
```

**File:** `src/app/components/footer/footer.component.html`

```html
<footer class="app-footer" *ngIf="settings$ | async as settings">
  <div class="footer-container">
    <!-- Footer Text -->
    <div class="footer-text" *ngIf="settings.footerText">
      <p>{{ settings.footerText }}</p>
    </div>

    <!-- Footer Info -->
    <div class="footer-info">
      <!-- Address -->
      <div class="footer-item" *ngIf="settings.footerAddress">
        <i class="icon-location"></i>
        <span>{{ settings.footerAddress }}</span>
      </div>

      <!-- Email -->
      <div class="footer-item" *ngIf="settings.footerEmail">
        <i class="icon-email"></i>
        <a [href]="'mailto:' + settings.footerEmail">{{ settings.footerEmail }}</a>
      </div>

      <!-- Phone -->
      <div class="footer-item" *ngIf="settings.footerPhone">
        <i class="icon-phone"></i>
        <a [href]="'tel:' + settings.footerPhone">{{ settings.footerPhone }}</a>
      </div>

      <!-- Website -->
      <div class="footer-item" *ngIf="settings.footerWebsite">
        <i class="icon-website"></i>
        <a [href]="settings.footerWebsite" target="_blank" rel="noopener noreferrer">Visit Website</a>
      </div>
    </div>

    <!-- Copyright -->
    <div class="footer-copyright" *ngIf="settings.footerCopyright">
      <p>{{ settings.footerCopyright }}</p>
    </div>
  </div>
</footer>
```

**File:** `src/app/components/footer/footer.component.css`

```css
.app-footer {
  background-color: #2c3e50;
  color: #ecf0f1;
  padding: 2rem;
  margin-top: auto;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
}

.footer-text {
  margin-bottom: 1.5rem;
}

.footer-text p {
  margin: 0;
  font-size: 1rem;
  line-height: 1.6;
}

.footer-info {
  display: flex;
  flex-wrap: wrap;
  gap: 2rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.footer-item i {
  font-size: 1.1rem;
}

.footer-item a {
  color: #ecf0f1;
  text-decoration: none;
  transition: color 0.3s;
}

.footer-item a:hover {
  color: #3498db;
}

.footer-copyright {
  text-align: center;
  padding-top: 1rem;
  font-size: 0.85rem;
  opacity: 0.8;
}

.footer-copyright p {
  margin: 0;
}
```

### Step 4: Use in App Component

**File:** `src/app/app.component.html`

```html
<div class="app-container">
  <!-- Dynamic Header -->
  <app-header></app-header>

  <!-- Main Content -->
  <main class="main-content">
    <router-outlet></router-outlet>
  </main>

  <!-- Dynamic Footer -->
  <app-footer></app-footer>
</div>
```

**File:** `src/app/app.component.css`

```css
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  padding: 2rem;
}
```

### Step 5: Register Components

**File:** `src/app/app.module.ts` (or standalone components)

```typescript
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { AppComponent } from './app.component';
import { HeaderComponent } from './components/header/header.component';
import { FooterComponent } from './components/footer/footer.component';
import { SystemSettingsService } from './services/system-settings.service';

@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    FooterComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule
  ],
  providers: [SystemSettingsService],
  bootstrap: [AppComponent]
})
export class AppModule { }
```

---

## React Implementation

### Step 1: Create Custom Hook

**File:** `src/hooks/useSystemSettings.ts`

```typescript
import { useState, useEffect } from 'react';
import axios from 'axios';

interface SystemSettings {
  id: number;
  logoUrl: string | null;
  logoHeader: string | null;
  logoSubheader: string | null;
  stateName: string | null;
  footerText: string | null;
  footerCopyright: string | null;
  footerAddress: string | null;
  footerEmail: string | null;
  footerPhone: string | null;
  footerWebsite: string | null;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

const API_URL = 'http://localhost:8080/api/admin/system-settings';

export const useSystemSettings = () => {
  const [settings, setSettings] = useState<SystemSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSettings = async () => {
      try {
        setLoading(true);
        const response = await axios.get<ApiResponse<SystemSettings>>(API_URL);
        if (response.data.success && response.data.data) {
          setSettings(response.data.data);
        }
        setError(null);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to load settings');
        console.error('Failed to load system settings:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchSettings();
  }, []);

  return { settings, loading, error };
};
```

### Step 2: Create Header Component

**File:** `src/components/Header.tsx`

```typescript
import React from 'react';
import { useSystemSettings } from '../hooks/useSystemSettings';
import './Header.css';

const Header: React.FC = () => {
  const { settings, loading } = useSystemSettings();

  if (loading) {
    return (
      <header className="app-header">
        <div className="header-container">
          <div>Loading...</div>
        </div>
      </header>
    );
  }

  return (
    <header className="app-header">
      <div className="header-container">
        {/* Logo */}
        <div className="logo-section">
          {settings?.logoUrl && (
            <img
              src={settings.logoUrl}
              alt="Logo"
              className="logo-image"
              onError={(e) => {
                (e.target as HTMLImageElement).style.display = 'none';
              }}
            />
          )}
        </div>

        {/* Header Text */}
        <div className="header-text-section">
          <h1 className="logo-header">
            {settings?.logoHeader || 'Revenue & Settlement Department'}
          </h1>
          <h2 className="logo-subheader">
            {settings?.logoSubheader || 'Government of Manipur'}
          </h2>
          <span className="state-name">
            {settings?.stateName || 'Manipur'}
          </span>
        </div>
      </div>
    </header>
  );
};

export default Header;
```

**File:** `src/components/Header.css`

```css
.app-header {
  background-color: #fff;
  border-bottom: 2px solid #e0e0e0;
  padding: 1rem 2rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.header-container {
  display: flex;
  align-items: center;
  gap: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.logo-section {
  flex-shrink: 0;
}

.logo-image {
  max-height: 80px;
  width: auto;
  object-fit: contain;
}

.header-text-section {
  flex: 1;
}

.logo-header {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: #1a1a1a;
}

.logo-subheader {
  margin: 0.25rem 0;
  font-size: 1.1rem;
  font-weight: 400;
  color: #666;
}

.state-name {
  display: block;
  font-size: 0.9rem;
  color: #888;
  margin-top: 0.25rem;
}
```

### Step 3: Create Footer Component

**File:** `src/components/Footer.tsx`

```typescript
import React from 'react';
import { useSystemSettings } from '../hooks/useSystemSettings';
import './Footer.css';

const Footer: React.FC = () => {
  const { settings, loading } = useSystemSettings();

  if (loading || !settings) {
    return null;
  }

  return (
    <footer className="app-footer">
      <div className="footer-container">
        {/* Footer Text */}
        {settings.footerText && (
          <div className="footer-text">
            <p>{settings.footerText}</p>
          </div>
        )}

        {/* Footer Info */}
        <div className="footer-info">
          {/* Address */}
          {settings.footerAddress && (
            <div className="footer-item">
              <i className="icon-location">📍</i>
              <span>{settings.footerAddress}</span>
            </div>
          )}

          {/* Email */}
          {settings.footerEmail && (
            <div className="footer-item">
              <i className="icon-email">✉️</i>
              <a href={`mailto:${settings.footerEmail}`}>{settings.footerEmail}</a>
            </div>
          )}

          {/* Phone */}
          {settings.footerPhone && (
            <div className="footer-item">
              <i className="icon-phone">📞</i>
              <a href={`tel:${settings.footerPhone}`}>{settings.footerPhone}</a>
            </div>
          )}

          {/* Website */}
          {settings.footerWebsite && (
            <div className="footer-item">
              <i className="icon-website">🌐</i>
              <a
                href={settings.footerWebsite}
                target="_blank"
                rel="noopener noreferrer"
              >
                Visit Website
              </a>
            </div>
          )}
        </div>

        {/* Copyright */}
        {settings.footerCopyright && (
          <div className="footer-copyright">
            <p>{settings.footerCopyright}</p>
          </div>
        )}
      </div>
    </footer>
  );
};

export default Footer;
```

**File:** `src/components/Footer.css`

```css
.app-footer {
  background-color: #2c3e50;
  color: #ecf0f1;
  padding: 2rem;
  margin-top: auto;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
}

.footer-text {
  margin-bottom: 1.5rem;
}

.footer-text p {
  margin: 0;
  font-size: 1rem;
  line-height: 1.6;
}

.footer-info {
  display: flex;
  flex-wrap: wrap;
  gap: 2rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.footer-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
}

.footer-item i {
  font-size: 1.1rem;
}

.footer-item a {
  color: #ecf0f1;
  text-decoration: none;
  transition: color 0.3s;
}

.footer-item a:hover {
  color: #3498db;
}

.footer-copyright {
  text-align: center;
  padding-top: 1rem;
  font-size: 0.85rem;
  opacity: 0.8;
}

.footer-copyright p {
  margin: 0;
}
```

### Step 4: Use in App Component

**File:** `src/App.tsx`

```typescript
import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Header from './components/Header';
import Footer from './components/Footer';
import './App.css';

function App() {
  return (
    <div className="app-container">
      <Header />
      <main className="main-content">
        <Router>
          <Routes>
            {/* Your routes here */}
          </Routes>
        </Router>
      </main>
      <Footer />
    </div>
  );
}

export default App;
```

**File:** `src/App.css`

```css
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  padding: 2rem;
}
```

---

## Key Points

1. **No Authentication Required**: The GET endpoint is public, so no token is needed.

2. **Fallback Values**: Always provide fallback values in case settings are null or API fails.

3. **Error Handling**: Handle loading states and errors gracefully.

4. **Image Error Handling**: Hide logo image if it fails to load.

5. **Conditional Rendering**: Only show fields that have values.

6. **Responsive Design**: Use CSS flexbox/grid for responsive layouts.

7. **Caching**: Consider caching settings in localStorage to reduce API calls.

---

## Optional: Add Caching

**Angular Service Enhancement:**

```typescript
loadSettings(): void {
  // Check cache first
  const cached = localStorage.getItem('systemSettings');
  if (cached) {
    try {
      const settings = JSON.parse(cached);
      this.settingsSubject.next(settings);
    } catch (e) {
      console.error('Failed to parse cached settings', e);
    }
  }

  // Fetch from API
  this.http.get<ApiResponse<SystemSettings>>(this.apiUrl).subscribe({
    next: (response) => {
      if (response.success && response.data) {
        this.settingsSubject.next(response.data);
        // Cache for 1 hour
        localStorage.setItem('systemSettings', JSON.stringify(response.data));
        localStorage.setItem('systemSettingsTime', Date.now().toString());
      }
    },
    error: (error) => {
      console.error('Failed to load system settings:', error);
    }
  });
}
```

**React Hook Enhancement:**

```typescript
useEffect(() => {
  const fetchSettings = async () => {
    // Check cache first
    const cached = localStorage.getItem('systemSettings');
    const cacheTime = localStorage.getItem('systemSettingsTime');
    const oneHour = 60 * 60 * 1000;

    if (cached && cacheTime && Date.now() - parseInt(cacheTime) < oneHour) {
      try {
        setSettings(JSON.parse(cached));
        setLoading(false);
        return;
      } catch (e) {
        console.error('Failed to parse cached settings', e);
      }
    }

    // Fetch from API
    try {
      setLoading(true);
      const response = await axios.get<ApiResponse<SystemSettings>>(API_URL);
      if (response.data.success && response.data.data) {
        setSettings(response.data.data);
        localStorage.setItem('systemSettings', JSON.stringify(response.data.data));
        localStorage.setItem('systemSettingsTime', Date.now().toString());
      }
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load settings');
    } finally {
      setLoading(false);
    }
  };

  fetchSettings();
}, []);
```

---

## Testing

1. **Start Backend**: Ensure backend is running on `http://localhost:8080`
2. **Test API**: Visit `http://localhost:8080/api/admin/system-settings` in browser
3. **Check Components**: Header and footer should display settings from API
4. **Update Settings**: Use admin panel to update settings, refresh frontend to see changes

---

**That's it!** Your header and footer are now dynamic and will automatically update when admin changes settings in the backend.

