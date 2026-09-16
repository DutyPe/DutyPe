type SiteIconProps = {
  name: string;
  className?: string;
};

export function SiteIcon({ name, className = "" }: SiteIconProps) {
  return (
    <svg
      className={`site-icon ${className}`}
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <use href={`/icons.svg#${name}`} />
    </svg>
  );
}