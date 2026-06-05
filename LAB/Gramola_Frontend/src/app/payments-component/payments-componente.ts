
/* CHECK
 * Componente encargado de gestionar los pagos con Stripe.
 *
 * Este componente puede funcionar de dos formas:
 * - modo subscription: para pagar la suscripción del bar.
 * - modo song: para pagar una canción individual desde la pantalla de música.
 *
 * Carga dinámicamente el script de Stripe, muestra el formulario de tarjeta,
 * pide al backend que prepare el pago y después confirma la transacción.
 *
 * Cuando el pago termina correctamente, emite paymentSuccess para avisar
 * al componente padre.
 */import { Component, OnInit, Output, EventEmitter, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { PaymentService } from '../payment-service';
import { AppUrlService } from '../app-url.service';

declare let Stripe: any;

type Plan = {
  id: string;
  name: string;
  description?: string;
  amount: number;
  currency: string;
  recommended?: boolean;
  intervalType?: string;
  planType?: string;
};

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './payments-componente.html',
  styleUrls: ['./payments-componente.css']
})
export class PaymentComponent implements OnInit {

  @Input() mode: 'subscription' | 'song' = 'subscription';
  @Input() songAmountCents = 99;

  @Output() paymentSuccess = new EventEmitter<any>();

  private readonly STRIPE_PUBLISHABLE_KEY =
    'pk_test_51SIUyT8Zqpuv0BKmpQwGNT7RVbVvksxDfjmS1ovCTF2ENcKoNht1aZ6h7IhfW1I7chlhcJzxQ8sp2WPvjWEauEwC00MGGoosaK';

  stripe: any = null;
  card: any = null;

  paying = false;
  errorMsg: string | null = null;

  token = '';
  email = '';

  plans: Plan[] = [];
  songPlan: Plan | null = null;

  selectedPlanId: string | null = null;

  songPriceEuros = 0.88;

  constructor(
    private paymentService: PaymentService,
    private appUrlService: AppUrlService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';

    this.email =
      sessionStorage.getItem('email') ||
      localStorage.getItem('email') ||
      '';

    /*
     * Primero cargo Stripe desde la URL guardada en MySQL.
     * Cuando ya está cargado, preparo los planes o el pago de canción.
     */
    this.loadStripeScript();
  }

  /*
   * Cargo dinámicamente el script de Stripe.
   * Así la URL de Stripe ya no está escrita en index.html.
   */
  private loadStripeScript(): void {
    this.appUrlService.getUrl('STRIPE_JS_URL').subscribe({
      next: (stripeJsUrl) => {
        this.insertStripeScript(stripeJsUrl)
          .then(() => {
            this.stripe = Stripe(this.STRIPE_PUBLISHABLE_KEY);
            this.loadPaymentData();
          })
          .catch(() => {
            this.errorMsg = 'Stripe no se ha podido cargar.';
          });
      },
      error: () => {
        this.errorMsg = 'No se pudo cargar la URL de Stripe desde la base de datos.';
      }
    });
  }

  /*
   * Inserto el script solo si no estaba ya cargado.
   * Esto evita duplicar el script si el componente se abre varias veces.
   */
  private insertStripeScript(src: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (typeof Stripe !== 'undefined') {
        resolve();
        return;
      }

      const existingScript = document.querySelector(
        `script[src="${src}"]`
      ) as HTMLScriptElement | null;

      if (existingScript) {
        existingScript.addEventListener('load', () => resolve());
        existingScript.addEventListener('error', () => reject());
        return;
      }

      const script = document.createElement('script');
      script.src = src;
      script.async = true;

      script.onload = () => resolve();
      script.onerror = () => reject();

      document.body.appendChild(script);
    });
  }

  /*
   * Cargo datos distintos según si estoy pagando suscripción o canción.
   */
  private loadPaymentData(): void {
    if (this.mode === 'song') {
      this.loadSongPlan();
    } else {
      this.loadPlans();
    }
  }

  loadPlans(): void {
    this.paymentService.getPlans().subscribe({
      next: (data: any[]) => {
        this.plans = (data || []) as Plan[];
        this.selectedPlanId = this.plans.length > 0 ? this.plans[0].id : null;

        setTimeout(() => this.mountCardIfNeeded(), 100);
      },
      error: (err: any) => {
        console.error(err);
        this.errorMsg = 'No se pudieron cargar los planes.';
      }
    });
  }

  loadSongPlan(): void {
    this.paymentService.getSongPlan().subscribe({
      next: (p: any) => {
        this.songPlan = p as Plan;
        this.selectedPlanId = this.songPlan?.id ?? 'song';

        setTimeout(() => this.mountCardIfNeeded(), 100);
      },
      error: (err: any) => {
        console.error(err);

        this.songPlan = null;
        this.selectedPlanId = 'song';

        setTimeout(() => this.mountCardIfNeeded(), 100);
      }
    });
  }

  selectPlan(planId: string): void {
    this.selectedPlanId = planId;
    this.errorMsg = null;

    setTimeout(() => this.mountCardIfNeeded(), 100);
  }

  /*
   * Pinto el formulario de tarjeta de Stripe.
   * Si todavía no existe el div en el HTML, lo vuelvo a intentar.
   */
  private mountCardIfNeeded(): void {
    if (this.card) {
      return;
    }

    if (!this.stripe) {
      return;
    }

    const el = document.getElementById('card-element');

    if (!el) {
      setTimeout(() => this.mountCardIfNeeded(), 100);
      return;
    }

    const elements = this.stripe.elements();

    this.card = elements.create('card', {
      style: {
        base: {
          fontSize: '16px',
          color: '#111827',
          '::placeholder': {
            color: '#9ca3af'
          }
        },
        invalid: {
          color: '#b91c1c'
        }
      }
    });

    this.card.mount('#card-element');
  }

  getSongPriceCentsFromInput(): number {
    const value = Number(this.songPriceEuros);

    if (Number.isNaN(value) || value <= 0) {
      return 88;
    }

    return Math.round(value * 100);
  }

  pay(): void {
    if (!this.selectedPlanId) {
      this.errorMsg = 'Selecciona un plan antes de pagar.';
      return;
    }

    if (this.mode === 'subscription' && !this.token) {
      this.errorMsg = 'No se encontró token. Entra desde el enlace del correo para pagar la suscripción.';
      return;
    }

    if (!this.card) {
      this.errorMsg = 'No se pudo cargar el formulario de tarjeta.';
      this.mountCardIfNeeded();
      return;
    }

    const plan = this.selectedPlanId;

    const token =
      this.mode === 'subscription'
        ? this.token || undefined
        : undefined;

    const email =
      this.mode === 'song' && this.email && this.email.trim().length > 0
        ? this.email.trim()
        : undefined;

    const songPriceCents =
      this.mode === 'subscription'
        ? this.getSongPriceCentsFromInput()
        : undefined;

    this.paying = true;
    this.errorMsg = null;

    this.paymentService.prepay(plan, token, email, songPriceCents).subscribe({
      next: async (transactionDetails: any) => {
        try {
          const clientSecret = transactionDetails?.data?.client_secret;
          const transactionId = transactionDetails?.id;

          if (!clientSecret || !transactionId) {
            this.paying = false;
            this.errorMsg = 'Respuesta de pago inválida.';
            return;
          }

          const result = await this.stripe.confirmCardPayment(clientSecret, {
            payment_method: {
              card: this.card
            }
          });

          if (result.error) {
            this.paying = false;
            this.errorMsg = result.error.message || 'Error en el pago.';
            return;
          }

          this.paymentService.confirm(result, transactionId, token).subscribe({
            next: () => {
              this.paying = false;
              this.paymentSuccess.emit({ ok: true });

              if (this.mode === 'subscription') {
                this.router.navigate(['/']);
              }
            },
            error: (err: any) => {
              console.error(err);
              this.paying = false;
              this.errorMsg = 'El pago no se completó.';
            }
          });

        } catch (e: any) {
          console.error(e);
          this.paying = false;
          this.errorMsg = 'Error procesando el pago.';
        }
      },
      error: (err: any) => {
        console.error(err);
        this.paying = false;
        this.errorMsg = 'Error preparando el pago.';
      }
    });
  }

  formatPrice(amountCents: number, currency: string): string {
    const value = (amountCents / 100).toFixed(2);
    return `${value} ${String(currency || 'eur').toUpperCase()}`;
  }
}