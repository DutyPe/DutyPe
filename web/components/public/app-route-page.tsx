import { AppLaunchCard } from "@/components/public/app-launch-card";
import { type DeepLinkKind } from "@/lib/public-site";

type Props = {
  kind: DeepLinkKind;
  entityId?: string;
  eyebrow: string;
  title: string;
  description: string;
  bullets: string[];
  icon?: string;
  valueLabel?: string;
  value?: string;
  valueHint?: string;
  launchHeadline?: string;
  launchDescription?: string;
};

export function AppRoutePage({
  kind,
  entityId,
  eyebrow,
  title,
  description,
  bullets,
  icon = "Open",
  valueLabel,
  value,
  valueHint,
  launchHeadline = "Open in DutyPe App",
  launchDescription = "If the app does not open automatically, use the store link below."
}: Props) {
  return (
    <div className="page-shell bridge-shell">
      <div className="page-ambient ambient-a" />
      <div className="page-ambient ambient-b" />

      <div className="page-wrap bridge-wrap">
        <section className="bridge-panel card">
          <div className="bridge-header">
            <span className="bridge-brand">DutyPe</span>
            <span className="eyebrow">{eyebrow}</span>
          </div>

          <div className="bridge-icon" aria-hidden="true">
            {icon}
          </div>

          <div className="bridge-copy">
            <h1>{title}</h1>
            <p>{description}</p>
          </div>

          {valueLabel && value ? (
            <div className="bridge-value">
              <span>{valueLabel}</span>
              <strong>{value}</strong>
              {valueHint ? <small>{valueHint}</small> : null}
            </div>
          ) : null}

          <AppLaunchCard
            kind={kind}
            entityId={entityId}
            headline={launchHeadline}
            description={launchDescription}
            bullets={bullets}
            badge="Opening in app"
          />

          <p className="bridge-note">
            DutyPe connects workers with employers directly. Download the app for the full experience.
          </p>
        </section>
      </div>
    </div>
  );
}
