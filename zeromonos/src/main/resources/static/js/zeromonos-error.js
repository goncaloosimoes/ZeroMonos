/**
 * error.js - Página de Erro
 * ZeroMonos - Sistema de Recolha de Resíduos Volumosos
 */

// Aguardar o carregamento completo da página
document.addEventListener('DOMContentLoaded', function () {
    console.log('ZeroMonos - Página de erro carregada');

    loadErrorMessage();
});

/**
* Carrega mensagem de erro da URL ou da sessão
*/
function loadErrorMessage() {
    const errorDetails = document.getElementById('error-details');

    // Tentar obter mensagem da URL
    const urlParams = new URLSearchParams(window.location.search);
    const errorMessage = urlParams.get('message') || urlParams.get('error');

    if (errorMessage) {
        // Decodificar mensagem da URL
        try {
            const decodedMessage = decodeURIComponent(errorMessage);
            errorDetails.textContent = decodedMessage;
        } catch (e) {
            errorDetails.textContent = errorMessage;
        }
    } else {
        // Tentar obter da sessão storage (se foi redirecionado de outra página)
        const sessionError = sessionStorage.getItem('errorMessage');
        if (sessionError) {
            errorDetails.textContent = sessionError;
            sessionStorage.removeItem('errorMessage');
        } else {
            // Mensagem padrão
            errorDetails.textContent = 'A operação não pôde ser concluída. Por favor, tente novamente.';
        }
    }
}

/**
* Função auxiliar para redirecionar para página de erro
* Pode ser chamada de outras páginas
*/
function redirectToError(message) {
    if (message) {
        sessionStorage.setItem('errorMessage', message);
    }
    window.location.href = '/error-page.html' + (message ? `?message=${encodeURIComponent(message)}` : '');
}

// Exportar função para uso global
window.redirectToError = redirectToError;
