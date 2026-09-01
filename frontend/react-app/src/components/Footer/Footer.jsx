import './Footer.scss';

/**
 * Global application footer. Rendered once by MainLayout, so it appears on every
 * authenticated page; the auth screen sits outside that layout and has none.
 */
const Footer = () => (
    <footer className="app-footer">
        <span className="app-footer__text">
            © {new Date().getFullYear()} AI Document Chat System
        </span>
    </footer>
);

export default Footer;
