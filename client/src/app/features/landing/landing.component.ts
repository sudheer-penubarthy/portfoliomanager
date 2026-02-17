import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { inject } from '@angular/core';
import { AuthService } from '@shared/services/auth.service';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule],
  template: `
    <div class="landing-wrapper">
      <!-- Header -->
      <header class="landing-header">
        <div class="header-container">
          <h1 class="logo">
            <mat-icon>trending_up</mat-icon>
            Portfolio Manager
          </h1>
          <nav class="header-nav">
            <button mat-stroked-button (click)="navigateTo('/auth/login')">
              <mat-icon>login</mat-icon>
              Login
            </button>
            <button mat-raised-button color="primary" (click)="navigateTo('/auth/register')">
              <mat-icon>person_add</mat-icon>
              Register
            </button>
          </nav>
        </div>
      </header>

      <!-- Main Content -->
      <main class="landing-main">
        <section class="hero">
          <div class="hero-content">
            <h2>Manage Your Investment Portfolio</h2>
            <p>Track, analyze, and optimize your investment portfolio with ease</p>
            <button mat-raised-button color="primary" size="large" (click)="navigateTo('/auth/login')">
              Get Started
            </button>
          </div>
        </section>

        <!-- Features Section -->
        <section class="features">
          <h3>Key Features</h3>
          <div class="features-grid">
            <div class="feature-card">
              <mat-icon>cloud_upload</mat-icon>
              <h4>Easy Upload</h4>
              <p>Import transactions from multiple sources with our simple upload interface</p>
            </div>

            <div class="feature-card">
              <mat-icon>assessment</mat-icon>
              <h4>Portfolio View</h4>
              <p>Get a comprehensive overview of your entire investment portfolio</p>
            </div>

            <div class="feature-card">
              <mat-icon>trending_up</mat-icon>
              <h4>Goal Tracking</h4>
              <p>Set and track your financial goals with progress monitoring</p>
            </div>

            <div class="feature-card">
              <mat-icon>insights</mat-icon>
              <h4>Analytics</h4>
              <p>Detailed analytics and reports to help you make informed decisions</p>
            </div>

            <div class="feature-card">
              <mat-icon>brightness_4</mat-icon>
              <h4>Dark Mode</h4>
              <p>Comfortable viewing with automatic light and dark theme support</p>
            </div>

            <div class="feature-card">
              <mat-icon>security</mat-icon>
              <h4>Secure</h4>
              <p>Your data is protected with industry-standard security measures</p>
            </div>
          </div>
        </section>

        <!-- CTA Section -->
        <section class="cta">
          <h3>Ready to Get Started?</h3>
          <p>Join thousands of investors managing their portfolios efficiently</p>
          <div class="cta-buttons">
            <button mat-raised-button color="primary" (click)="navigateTo('/auth/login')">
              Sign In
            </button>
            <button mat-stroked-button (click)="navigateTo('/auth/register')">
              Create Account
            </button>
          </div>
        </section>
      </main>

      <!-- Footer -->
      <footer class="landing-footer">
        <p>&copy; 2026 Portfolio Manager. All rights reserved.</p>
      </footer>
    </div>
  `,
  styles: [`
    .landing-wrapper {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
      background-color: var(--color-bg);
      color: var(--color-text-primary);
    }

    /* Header */
    .landing-header {
      background-color: var(--color-bg);
      border-bottom: 1px solid var(--color-border);
      padding: 16px 0;
      position: sticky;
      top: 0;
      z-index: 1000;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .header-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .logo {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 24px;
      font-weight: 600;
      margin: 0;
      color: var(--color-primary);
    }

    .header-nav {
      display: flex;
      gap: 12px;
    }

    /* Main Content */
    .landing-main {
      flex: 1;
      max-width: 1200px;
      margin: 0 auto;
      width: 100%;
      padding: 60px 20px;
    }

    /* Hero Section */
    .hero {
      text-align: center;
      margin-bottom: 80px;
    }

    .hero-content h2 {
      font-size: 48px;
      font-weight: 700;
      margin: 0 0 20px 0;
      color: var(--color-text-primary);
    }

    .hero-content p {
      font-size: 20px;
      color: var(--color-text-secondary);
      margin: 0 0 30px 0;
      max-width: 600px;
      margin-left: auto;
      margin-right: auto;
    }

    /* Features Section */
    .features {
      margin-bottom: 80px;
    }

    .features h3 {
      font-size: 36px;
      text-align: center;
      margin: 0 0 40px 0;
      color: var(--color-text-primary);
    }

    .features-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 30px;
    }

    .feature-card {
      background-color: var(--color-card-bg);
      border: 1px solid var(--color-border);
      border-radius: 8px;
      padding: 30px;
      text-align: center;
      transition: transform 150ms ease, box-shadow 150ms ease;
    }

    .feature-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
    }

    .feature-card mat-icon {
      font-size: 48px;
      height: 48px;
      width: 48px;
      color: var(--color-primary);
      margin-bottom: 16px;
    }

    .feature-card h4 {
      font-size: 18px;
      font-weight: 600;
      margin: 0 0 12px 0;
      color: var(--color-text-primary);
    }

    .feature-card p {
      font-size: 14px;
      color: var(--color-text-secondary);
      margin: 0;
      line-height: 1.6;
    }

    /* CTA Section */
    .cta {
      text-align: center;
      background-color: var(--color-card-bg);
      border: 1px solid var(--color-border);
      border-radius: 8px;
      padding: 60px 40px;
      margin-bottom: 40px;
    }

    .cta h3 {
      font-size: 32px;
      margin: 0 0 16px 0;
      color: var(--color-text-primary);
    }

    .cta p {
      font-size: 16px;
      color: var(--color-text-secondary);
      margin: 0 0 30px 0;
    }

    .cta-buttons {
      display: flex;
      gap: 16px;
      justify-content: center;
      flex-wrap: wrap;
    }

    /* Footer */
    .landing-footer {
      background-color: var(--color-bg);
      border-top: 1px solid var(--color-border);
      text-align: center;
      padding: 20px;
      color: var(--color-text-secondary);
      font-size: 14px;
    }

    /* Responsive */
    @media (max-width: 768px) {
      .header-container {
        flex-direction: column;
        gap: 16px;
      }

      .hero-content h2 {
        font-size: 32px;
      }

      .hero-content p {
        font-size: 16px;
      }

      .features h3 {
        font-size: 28px;
      }

      .features-grid {
        grid-template-columns: 1fr;
      }

      .cta {
        padding: 40px 20px;
      }

      .cta h3 {
        font-size: 24px;
      }
    }
  `]
})
export class LandingComponent {
  private router = inject(Router);
  private authService = inject(AuthService);

  navigateTo(path: string): void {
    this.router.navigate([path]);
  }
}

