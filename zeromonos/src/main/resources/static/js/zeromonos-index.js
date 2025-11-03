/**
 * main.js - Página Inicial
 * ZeroMonos - Sistema de Recolha de Resíduos Volumosos
 */

// Aguardar o carregamento completo da página
document.addEventListener('DOMContentLoaded', function () {
    console.log('ZeroMonos - Página inicial carregada');

    // Animações suaves para os cards
    initCardAnimations();

    // Adicionar efeitos interativos nos botões
    initButtonEffects();
});

/**
* Inicializa animações nos cards
*/
function initCardAnimations() {
    const cards = document.querySelectorAll('.card-feature');

    // Adicionar observador de interseção para animações ao entrar na viewport
    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry, index) => {
            if (entry.isIntersecting) {
                setTimeout(() => {
                    entry.target.style.opacity = '1';
                    entry.target.style.transform = 'translateY(0)';
                }, index * 100);
                observer.unobserve(entry.target);
            }
        });
    }, {
        threshold: 0.1
    });

    cards.forEach(card => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(20px)';
        card.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
        observer.observe(card);
    });
}

/**
* Inicializa efeitos interativos nos botões
*/
function initButtonEffects() {
    const buttons = document.querySelectorAll('.btn');

    buttons.forEach(button => {
        // Efeito de ripple ao clicar
        button.addEventListener('click', function (e) {
            const ripple = document.createElement('span');
            const rect = this.getBoundingClientRect();
            const size = Math.max(rect.width, rect.height);
            const x = e.clientX - rect.left - size / 2;
            const y = e.clientY - rect.top - size / 2;

            ripple.style.width = ripple.style.height = size + 'px';
            ripple.style.left = x + 'px';
            ripple.style.top = y + 'px';
            ripple.classList.add('ripple');

            this.appendChild(ripple);

            setTimeout(() => {
                ripple.remove();
            }, 600);
        });
    });
}

// Adicionar estilo para o efeito ripple se não existir
if (!document.getElementById('ripple-style')) {
    const style = document.createElement('style');
    style.id = 'ripple-style';
    style.textContent = `
      .btn {
          position: relative;
          overflow: hidden;
      }
      .btn > * {
          position: relative;
          z-index: 1;
      }
      .btn .ripple {
          position: absolute;
          border-radius: 50%;
          background: rgba(255, 255, 255, 0.6);
          transform: scale(0);
          animation: ripple-animation 0.6s ease-out;
          pointer-events: none;
          z-index: 0;
      }
      @keyframes ripple-animation {
          to {
              transform: scale(4);
              opacity: 0;
          }
      }
  `;
    document.head.appendChild(style);
}
