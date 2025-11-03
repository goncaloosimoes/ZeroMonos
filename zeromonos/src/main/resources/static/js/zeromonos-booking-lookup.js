/**
 * booking-view.js - Consultar Agendamento
 * ZeroMonos - Sistema de Recolha de Resíduos Volumosos
 */

const API_BASE = '/api/bookings';

// Aguardar o carregamento completo da página
document.addEventListener('DOMContentLoaded', function () {
    console.log('ZeroMonos - Consultar reserva carregado');

    initForm();
    checkURLToken();
});

/**
 * Inicializa o formulário de busca
 */
function initForm() {
    const form = document.getElementById('search-form-internal');
    const tokenInput = document.getElementById('token');

    // Submissão do formulário
    form.addEventListener('submit', async function (e) {
        e.preventDefault();
        const token = tokenInput.value.trim();

        if (!token) {
            showMessage('Por favor, insira um token válido.', 'error');
            tokenInput.focus();
            return;
        }

        await searchBooking(token);
    });

    // Botão de busca (caso seja clicado diretamente)
    const searchBtn = document.getElementById('search-btn');
    searchBtn.addEventListener('click', async function (e) {
        e.preventDefault();
        form.dispatchEvent(new Event('submit'));
    });

    // Buscar reserva ao pressionar Enter
    tokenInput.addEventListener('keypress', function (e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            form.dispatchEvent(new Event('submit'));
        }
    });
}

/**
 * Verifica se há token na URL
 */
function checkURLToken() {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');

    if (token) {
        document.getElementById('token').value = token;
        searchBooking(token);
    }
}

/**
 * Busca uma reserva pelo token
 */
async function searchBooking(token) {
    console.log('🔍 [BOOKING-VIEW] Iniciando busca por token...');
    console.log('🔍 [BOOKING-VIEW] Token:', token);
    console.log('🔍 [BOOKING-VIEW] Token tipo:', typeof token);
    console.log('🔍 [BOOKING-VIEW] Token length:', token ? token.length : 0);

    const searchBtn = document.getElementById('search-btn');
    const searchForm = document.querySelector('.search-form');
    const detailsSection = document.getElementById('booking-details');
    const cancelBtn = document.getElementById('cancel-btn');

    if (!token || token.trim().length === 0) {
        console.error('❌ [BOOKING-VIEW] Token inválido ou vazio');
        showMessage('Por favor, insira um token válido.', 'error');
        return;
    }

    const cleanToken = token.trim();
    const url = `${API_BASE}/${cleanToken}`;

    console.log('📡 [BOOKING-VIEW] Fazendo fetch para:', url);
    console.log('📡 [BOOKING-VIEW] URL completa:', window.location.origin + url);

    try {
        searchBtn.disabled = true;
        searchBtn.innerHTML = '<span>⏳</span><span>Buscando...</span>';
        searchForm.classList.add('loading');
        hideMessage();
        hideDetails();

        const response = await fetch(url);

        console.log('📥 [BOOKING-VIEW] Resposta recebida:');
        console.log('  - Status:', response.status);
        console.log('  - Status Text:', response.statusText);
        console.log('  - OK:', response.ok);
        console.log('  - Headers:', Object.fromEntries(response.headers.entries()));

        if (!response.ok) {
            let errorData = null;
            let errorText = '';

            try {
                errorData = await response.json();
                console.error('❌ [BOOKING-VIEW] Erro JSON:', errorData);
            } catch (e) {
                try {
                    errorText = await response.text();
                    console.error('❌ [BOOKING-VIEW] Erro texto:', errorText);
                } catch (e2) {
                    console.error('❌ [BOOKING-VIEW] Não foi possível ler o corpo da resposta');
                }
            }

            const errorMessage = errorData?.message || errorText || `Erro ${response.status}: ${response.statusText}`;
            console.error('❌ [BOOKING-VIEW] Lançando erro:', errorMessage);
            throw new Error(errorMessage);
        }

        console.log('📋 [BOOKING-VIEW] Parseando JSON...');
        const data = await response.json();
        console.log('📋 [BOOKING-VIEW] Dados recebidos:', data);
        console.log('📋 [BOOKING-VIEW] Tipo dos dados:', typeof data);
        console.log('📋 [BOOKING-VIEW] Token na resposta:', data.token);
        console.log('📋 [BOOKING-VIEW] Status na resposta:', data.status);

        // Validar dados recebidos
        if (!data.token) {
            console.error('❌ [BOOKING-VIEW] Token não encontrado nos dados recebidos');
            throw new Error('Dados inválidos recebidos do servidor');
        }

        // Mostrar detalhes da reserva
        console.log('📝 [BOOKING-VIEW] Exibindo detalhes da reserva...');
        displayBookingDetails(data);
        detailsSection.classList.remove('hidden');

        // Configurar botão de cancelamento
        console.log('🔘 [BOOKING-VIEW] Configurando botão de cancelamento...');
        setupCancelButton(data.token, data.status);

        // Scroll suave até os detalhes
        setTimeout(() => {
            detailsSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }, 100);

        console.log('✅ [BOOKING-VIEW] Busca concluída com sucesso');

    } catch (error) {
        console.error('❌ [BOOKING-VIEW] Erro completo ao buscar reserva:');
        console.error('  - Nome do erro:', error.name);
        console.error('  - Mensagem:', error.message);
        console.error('  - Stack:', error.stack);

        if (error instanceof TypeError) {
            console.error('  - Tipo: Erro de rede ou CORS');
        } else if (error instanceof SyntaxError) {
            console.error('  - Tipo: Erro ao parsear JSON');
        }

        showMessage(
            error.message || 'Erro ao buscar reserva. Verifique se o token está correto.',
            'error'
        );
    } finally {
        searchBtn.disabled = false;
        searchBtn.innerHTML = '<span>🔎</span><span>Buscar Agendamento</span>';
        searchForm.classList.remove('loading');
    }
}

/**
 * Mostra os detalhes da reserva
 */
function displayBookingDetails(booking) {
    console.log('📋 [BOOKING-VIEW] Exibindo detalhes:', booking);

    if (!booking) {
        console.error('❌ [BOOKING-VIEW] Booking é null ou undefined!');
        return;
    }

    const detailsGrid = document.getElementById('details-grid');
    const statusIcon = document.querySelector('.status-icon');

    if (!detailsGrid) {
        console.error('❌ [BOOKING-VIEW] Elemento details-grid não encontrado!');
        return;
    }

    if (!statusIcon) {
        console.warn('⚠️ [BOOKING-VIEW] Elemento status-icon não encontrado!');
    }

    // Configurar ícone de status
    try {
        const statusClass = getStatusClass(booking.status);
        const statusIconText = getStatusIcon(booking.status);
        console.log('📊 [BOOKING-VIEW] Status class:', statusClass);
        console.log('📊 [BOOKING-VIEW] Status icon:', statusIconText);

        if (statusIcon) {
            statusIcon.className = 'status-icon ' + statusClass;
            statusIcon.textContent = statusIconText;
        }
    } catch (error) {
        console.error('❌ [BOOKING-VIEW] Erro ao configurar ícone de status:', error);
    }

    // Formatar dados
    const timeSlotLabels = {
        'EARLY_MORNING': '🌄 Madrugada (06:00 - 08:00)',
        'MORNING': '🌅 Manhã (08:00 - 12:00)',
        'AFTERNOON': '☀️ Tarde (12:00 - 16:00)',
        'EVENING': '🌇 Fim de tarde (16:00 - 20:00)',
        'NIGHT': '🌙 Noite (20:00 - 22:00)',
        'LATE_NIGHT': '🌃 Madrugada tardia (22:00 - 06:00)',
        'ANYTIME': '⏰ Qualquer hora'
    };

    const statusLabels = {
        'RECEIVED': 'Recebida',
        'ASSIGNED': 'Atribuída',
        'IN_PROGRESS': 'Em Progresso',
        'COMPLETED': 'Concluída',
        'CANCELLED': 'Cancelada'
    };

    // Formatar data
    const requestedDate = new Date(booking.requestedDate);
    const formattedDate = requestedDate.toLocaleDateString('pt-PT', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });

    // Formatar datas de criação e atualização
    const createdAt = booking.createdAt
        ? new Date(booking.createdAt).toLocaleString('pt-PT')
        : 'N/A';
    const updatedAt = booking.updatedAt
        ? new Date(booking.updatedAt).toLocaleString('pt-PT')
        : 'N/A';

    // Criar HTML dos detalhes
    detailsGrid.innerHTML = `
        <div class="detail-item">
            <span class="detail-label">Token</span>
            <span class="detail-value" style="display: flex; align-items: center; gap: 0.5rem;">
                <span class="code">${booking.token}</span>
                <button onclick="navigator.clipboard.writeText('${booking.token}'); this.innerHTML='✓'; setTimeout(() => this.innerHTML='📋', 2000);" 
                        style="background: var(--success, #10b981); color: white; border: none; padding: 0.25rem 0.5rem; border-radius: 0.25rem; cursor: pointer; font-size: 0.875rem; white-space: nowrap; flex-shrink: 0;"
                        title="Copiar token">
                    📋
                </button>
            </span>
        </div>
        
        <div class="detail-item">
            <span class="detail-label">Município</span>
            <span class="detail-value">${booking.municipalityName || 'N/A'}</span>
        </div>
        
        <div class="detail-item">
            <span class="detail-label">Data da Recolha</span>
            <span class="detail-value">${formattedDate}</span>
        </div>
        
        <div class="detail-item">
            <span class="detail-label">Período</span>
            <span class="detail-value">${timeSlotLabels[booking.timeSlot] || booking.timeSlot}</span>
        </div>
        
        <div class="detail-item">
            <span class="detail-label">Status</span>
            <span class="detail-value status">
                <span class="badge badge-${getStatusBadgeClass(booking.status)}">${statusLabels[booking.status] || booking.status}</span>
            </span>
        </div>
        
        <div class="detail-item">
            <span class="detail-label">Descrição</span>
            <span class="detail-value">${escapeHtml(booking.description || 'N/A')}</span>
        </div>
        
        <div class="detail-item">
            <span class="detail-label">Criada em</span>
            <span class="detail-value">${createdAt}</span>
        </div>
        
        <div class="detail-item">
            <span class="detail-label">Atualizada em</span>
            <span class="detail-value">${updatedAt}</span>
        </div>
        
        ${booking.history && booking.history.length > 0 ? `
            <div class="detail-item" style="grid-column: 1 / -1;">
                <span class="detail-label">Histórico</span>
                <div class="detail-value">
                    <ul style="margin: 0.5rem 0; padding-left: 1.5rem; list-style-type: disc;">
                        ${booking.history.map(h => `<li>${escapeHtml(h)}</li>`).join('')}
                    </ul>
                </div>
            </div>
        ` : ''}
    `;
}

/**
 * Configura o botão de cancelamento
 */
function setupCancelButton(token, status) {
    const cancelBtn = document.getElementById('cancel-btn');

    // Remover event listeners anteriores
    const newCancelBtn = cancelBtn.cloneNode(true);
    cancelBtn.parentNode.replaceChild(newCancelBtn, cancelBtn);

    // Só permitir cancelamento se o status for RECEIVED ou ASSIGNED
    if (status === 'RECEIVED' || status === 'ASSIGNED') {
        newCancelBtn.disabled = false;
        newCancelBtn.style.display = 'flex';
        newCancelBtn.addEventListener('click', function (e) {
            e.preventDefault();
            cancelBooking(token);
        });
    } else {
        newCancelBtn.disabled = true;
        newCancelBtn.style.display = 'none';
    }
}

/**
 * Cancela uma reserva
 */
async function cancelBooking(token) {
    const cancelBtn = document.getElementById('cancel-btn');

    if (!confirm('Tem certeza que deseja cancelar este agendamento?')) {
        return;
    }

    try {
        cancelBtn.disabled = true;
        cancelBtn.innerHTML = '<span>⏳</span><span>Cancelando...</span>';

        const response = await fetch(`${API_BASE}/${token}/cancel`, {
            method: 'PUT'
        });

        if (!response.ok) {
            const data = await response.json();
            throw new Error(data.message || 'Erro ao cancelar reserva');
        }

        showMessage('Agendamento cancelado com sucesso!', 'success');

        // Recarregar detalhes da reserva
        setTimeout(() => {
            searchBooking(token);
        }, 1000);

    } catch (error) {
        console.error('Erro ao cancelar reserva:', error);
        showMessage(
            error.message || 'Erro ao cancelar agendamento. Por favor, tente novamente.',
            'error'
        );
    } finally {
        cancelBtn.disabled = false;
        cancelBtn.innerHTML = '<span>✕</span><span>Cancelar Agendamento</span>';
    }
}

/**
 * Retorna classe CSS para status
 */
function getStatusClass(status) {
    const statusMap = {
        'RECEIVED': 'success',
        'ASSIGNED': 'info',
        'IN_PROGRESS': 'warning',
        'COMPLETED': 'success',
        'CANCELLED': 'error'
    };
    return statusMap[status] || 'info';
}

/**
 * Retorna ícone para status
 */
function getStatusIcon(status) {
    const iconMap = {
        'RECEIVED': '📥',
        'ASSIGNED': '✅',
        'IN_PROGRESS': '⚙️',
        'COMPLETED': '✓',
        'CANCELLED': '✕'
    };
    return iconMap[status] || '❓';
}

/**
 * Retorna classe para badge de status
 */
function getStatusBadgeClass(status) {
    const badgeMap = {
        'RECEIVED': 'info',
        'ASSIGNED': 'info',
        'IN_PROGRESS': 'warning',
        'COMPLETED': 'success',
        'CANCELLED': 'error'
    };
    return badgeMap[status] || 'info';
}

/**
 * Mostra mensagem
 */
function showMessage(message, type = 'info') {
    const messageContainer = document.getElementById('message');

    const messageHTML = `
        <div class="message message-${type}">
            ${message}
        </div>
    `;

    messageContainer.innerHTML = messageHTML;

    // Scroll suave até a mensagem
    setTimeout(() => {
        messageContainer.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }, 100);
}

/**
 * Esconde mensagem
 */
function hideMessage() {
    const messageContainer = document.getElementById('message');
    messageContainer.innerHTML = '';
}

/**
 * Esconde detalhes
 */
function hideDetails() {
    const detailsSection = document.getElementById('booking-details');
    detailsSection.classList.add('hidden');
}

/**
 * Escapa HTML para prevenir XSS
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
