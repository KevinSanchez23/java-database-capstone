// footer.js

const footerScriptUrl = document.currentScript?.src || "/js/components/footer.js";
const footerLogoUrl = new URL("../../assets/images/logo/logo.png", footerScriptUrl).href;

function renderFooter() {
  const footer = document.getElementById("footer");

  if (!footer) {
    return;
  }

  const currentYear = new Date().getFullYear();

  footer.innerHTML = `
    <footer class="footer">
      <div class="footer-container">
        <div class="footer-logo">
          <img src="${footerLogoUrl}" alt="Smart Clinic logo">
          <p>&copy; Copyright ${currentYear}. All Rights Reserved by Smart Clinic.</p>
        </div>

        <nav class="footer-links" aria-label="Footer navigation">
          <div class="footer-column">
            <h4>Company</h4>
            <a href="#">About</a>
            <a href="#">Careers</a>
            <a href="#">Press</a>
          </div>

          <div class="footer-column">
            <h4>Support</h4>
            <a href="#">Account</a>
            <a href="#">Help Center</a>
            <a href="#">Contact</a>
          </div>

          <div class="footer-column">
            <h4>Legals</h4>
            <a href="#">Terms &amp; Conditions</a>
            <a href="#">Privacy Policy</a>
            <a href="#">Licensing</a>
          </div>
        </nav>
      </div>
    </footer>
  `;
}

window.renderFooter = renderFooter;

renderFooter();
