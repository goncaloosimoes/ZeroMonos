/**
 * staff-bookings.js - Painel Staff
 * ZeroMonos - Sistema de Gestão de Agendamentos de Recolha de Resíduos
 */

const API_BASE = '/api/bookings';
const STAFF_API_BASE = '/api/staff/bookings';

let allMunicipalities = [];
let currentBookings = [];

// Aguardar o carregamento completo da página
document.addEventListener('DOMContentLoaded', function () {
  console.log('ZeroMonos - Painel Staff carregado');

  initFilters();
  loadMunicipalities();
  loadBookings();
  initHistoryModal();
});

/**
 * Inicializa os filtros
 */
function initFilters() {
  const form = document.getElementById('filters-form');
  const resetBtn = document.getElementById('reset-btn');

  // Submissão do formulário de filtros
  form.addEventListener('submit', async function (e) {
    e.preventDefault();
    const municipality = document.getElementById('municipality-filter').value;
    await loadBookings(municipality || 'all');
  });

  // Botão de reset
  resetBtn.addEventListener('click', function () {
    form.reset();
    document.getElementById('municipality-filter').value = '';
    loadBookings();
  });
}

/**
 * Carrega a lista de municípios para o filtro
 */
async function loadMunicipalities() {
  try {
    const response = await fetch(`${API_BASE}/municipalities`);

    if (!response.ok) {
      throw new Error('Erro ao carregar municípios');
    }

    allMunicipalities = await response.json();
    const select = document.getElementById('municipality-filter');

    // Limpar opções antigas (exceto "Todos")
    while (select.children.length > 1) {
      select.removeChild(select.lastChild);
    }

    // Adicionar municípios
    allMunicipalities.forEach(municipality => {
      const option = document.createElement('option');
      option.value = municipality;
      option.textContent = municipality;
      select.appendChild(option);
    });

    console.log(`Carregados ${allMunicipalities.length} municípios para filtro`);

  } catch (error) {
    console.error('Erro ao carregar municípios:', error);
    showMessage('Erro ao carregar lista de municípios.', 'error');
  }
}

/**
 * Carrega a lista de agendamentos
 */
async function loadBookings(municipalityFilter = 'all') {
  const tbody = document.getElementById('bookings-tbody');
  const emptyState = document.getElementById('empty-state');
  const tableWrapper = document.querySelector('.table-wrapper');

  console.log('📋 [STAFF] Iniciando carregamento de agendamentos...');
  console.log('📋 [STAFF] Filtro de município:', municipalityFilter);

  try {
    // Mostrar estado de carregamento
    tbody.innerHTML = `
            <tr>
                <td colspan="6" class="loading-state">
                    <div class="loading-content">
                        <span class="loading-icon">⏳</span>
                        <span>Carregando agendamentos...</span>
                    </div>
                </td>
            </tr>
        `;

    // Construir URL com filtro
    const url = municipalityFilter && municipalityFilter !== 'all'
      ? `${STAFF_API_BASE}?municipality=${encodeURIComponent(municipalityFilter)}`
      : `${STAFF_API_BASE}?municipality=all`;

    console.log('📡 [STAFF] Fazendo fetch para:', url);
    console.log('📡 [STAFF] URL completa:', window.location.origin + url);

    const response = await fetch(url);

    console.log('📥 [STAFF] Resposta recebida:');
    console.log('  - Status:', response.status);
    console.log('  - Status Text:', response.statusText);
    console.log('  - OK:', response.ok);
    console.log('  - Headers:', Object.fromEntries(response.headers.entries()));

    // Clonar response antes de ler para poder usar novamente em caso de erro
    const responseClone = response.clone();

    if (!response.ok) {
      console.error('❌ [STAFF] Resposta com erro - Status:', response.status);

      let errorMessage = 'Erro ao carregar agendamentos';
      try {
        // Tentar ler como JSON primeiro
        const errorJson = await responseClone.json();
        console.error('❌ [STAFF] Erro JSON recebido:', errorJson);
        if (errorJson && errorJson.message) {
          errorMessage = errorJson.message;
          console.error('❌ [STAFF] Mensagem de erro do servidor:', errorJson.message);
        }
      } catch (jsonError) {
        // Se não conseguir parsear JSON, tentar como texto
        try {
          const responseClone2 = response.clone();
          const errorText = await responseClone2.text();
          console.error('❌ [STAFF] Erro texto recebido:', errorText);
          if (errorText && errorText.trim()) {
            errorMessage = errorText;
          }
        } catch (textError) {
          console.error('❌ [STAFF] Não foi possível ler o corpo da resposta:', textError);
          errorMessage = `Erro ${response.status}: ${response.statusText}`;
        }
      }

      throw new Error(errorMessage);
    }

    console.log('📋 [STAFF] Parseando JSON...');
    currentBookings = await response.json();
    console.log('📋 [STAFF] Dados recebidos:', currentBookings);
    console.log('📋 [STAFF] Tipo dos dados:', typeof currentBookings);
    console.log('📋 [STAFF] É array?', Array.isArray(currentBookings));

    if (!Array.isArray(currentBookings)) {
      console.error('❌ [STAFF] Dados não são um array:', currentBookings);
      throw new Error('Resposta inválida do servidor: não é um array');
    }

    console.log('📋 [STAFF] Número de agendamentos:', currentBookings.length);

    // Atualizar contador
    console.log('📊 [STAFF] Atualizando contador:', currentBookings.length);
    updateBookingCount(currentBookings.length);

    // Limpar tabela
    tbody.innerHTML = '';

    if (currentBookings.length === 0) {
      console.log('ℹ️ [STAFF] Nenhum agendamento encontrado');
      tableWrapper.style.display = 'none';
      emptyState.classList.remove('hidden');
    } else {
      console.log('✅ [STAFF] Adicionando agendamentos à tabela...');
      tableWrapper.style.display = 'block';
      emptyState.classList.add('hidden');

      // Adicionar agendamentos à tabela
      currentBookings.forEach((booking, index) => {
        console.log(`📝 [STAFF] Processando agendamento ${index + 1}:`, booking);
        try {
          const row = createBookingRow(booking);
          tbody.appendChild(row);
          console.log(`✅ [STAFF] Agendamento ${index + 1} adicionado com sucesso`);
        } catch (rowError) {
          console.error(`❌ [STAFF] Erro ao criar linha para agendamento ${index + 1}:`, rowError);
          console.error('  - Agendamento:', booking);
        }
      });

      console.log('✅ [STAFF] Todos os agendamentos foram adicionados à tabela');
    }

  } catch (error) {
    console.error('❌ [STAFF] Erro completo ao carregar agendamentos:');
    console.error('  - Nome do erro:', error.name);
    console.error('  - Mensagem:', error.message);
    console.error('  - Stack:', error.stack);

    if (error instanceof TypeError) {
      console.error('  - Tipo: Erro de rede ou CORS');
    } else if (error instanceof SyntaxError) {
      console.error('  - Tipo: Erro ao parsear JSON');
    }

    tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; color: var(--error); padding: 2rem;">
                    Erro ao carregar agendamentos: ${error.message || 'Erro desconhecido'}
                </td>
            </tr>
        `;
    showMessage('Erro ao carregar agendamentos: ' + (error.message || 'Erro desconhecido'), 'error');
  }
}

/**
 * Cria uma linha de tabela para uma reserva
 */
function createBookingRow(booking) {
  console.log('🔨 [STAFF] Criando linha para reserva:', booking);

  if (!booking) {
    console.error('❌ [STAFF] Booking é null ou undefined!');
    throw new Error('Booking inválido');
  }

  const tr = document.createElement('tr');

  try {
    // Formatar data
    console.log('📅 [STAFF] Data da reserva:', booking.requestedDate);
    const requestedDate = booking.requestedDate ? new Date(booking.requestedDate) : new Date();
    console.log('📅 [STAFF] Data parseada:', requestedDate);

    const formattedDate = requestedDate.toLocaleDateString('pt-PT', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
    console.log('📅 [STAFF] Data formatada:', formattedDate);

    // Formatar período
    const timeSlotLabels = {
      'EARLY_MORNING': '🌄 Madrugada',
      'MORNING': '🌅 Manhã',
      'AFTERNOON': '☀️ Tarde',
      'EVENING': '🌇 Fim de tarde',
      'NIGHT': '🌙 Noite',
      'LATE_NIGHT': '🌃 Madrugada tardia',
      'ANYTIME': '⏰ Qualquer hora'
    };

    console.log('⏰ [STAFF] TimeSlot:', booking.timeSlot);
    const timeSlotLabel = timeSlotLabels[booking.timeSlot] || booking.timeSlot;
    console.log('⏰ [STAFF] Label do TimeSlot:', timeSlotLabel);

    // Status labels
    const statusLabels = {
      'RECEIVED': 'Recebida',
      'ASSIGNED': 'Atribuída',
      'IN_PROGRESS': 'Em Progresso',
      'COMPLETED': 'Concluída',
      'CANCELLED': 'Cancelada'
    };

    console.log('📊 [STAFF] Status:', booking.status);

    // Badge de status
    const statusBadge = `
            <span class="badge badge-${getStatusBadgeClass(booking.status)}">
                ${statusLabels[booking.status] || booking.status}
            </span>
        `;

    // Botões de ação
    const actionButtons = createActionButtons(booking);
    console.log('🔘 [STAFF] Botões de ação criados');

    tr.innerHTML = `
            <td>
                <div style="display: flex; align-items: center; gap: 0.25rem;">
                    <code style="font-size: 0.75rem; word-break: break-all;">${escapeHtml(booking.token || 'N/A')}</code>
                    <button onclick="navigator.clipboard.writeText('${escapeHtml(booking.token)}'); this.innerHTML='✓'; setTimeout(() => this.innerHTML='📋', 2000);" 
                            style="background: var(--success, #10b981); color: white; border: none; padding: 0.125rem 0.25rem; border-radius: 0.25rem; cursor: pointer; font-size: 0.75rem; white-space: nowrap; flex-shrink: 0;"
                            title="Copiar token">
                        📋
                    </button>
                </div>
            </td>
            <td>${escapeHtml(booking.municipalityName || 'N/A')}</td>
            <td>${formattedDate}</td>
            <td>${timeSlotLabel}</td>
            <td>${statusBadge}</td>
            <td class="table-actions">${actionButtons}</td>
        `;

    console.log('✅ [STAFF] Linha criada com sucesso');
    return tr;
  } catch (error) {
    console.error('❌ [STAFF] Erro ao criar linha:', error);
    console.error('  - Booking completo:', booking);
    throw error;
  }
}

/**
 * Cria botões de ação para uma reserva
 */
function createActionButtons(booking) {
  const status = booking.status;
  const buttons = [];

  // Botão de histórico sempre disponível
  buttons.push(createHistoryButton(booking.token, booking.history));

  // Botões disponíveis baseados no status atual
  if (status === 'RECEIVED') {
    buttons.push(createStatusButton(booking.token, 'ASSIGNED', 'Atribuir', 'info'));
  }

  if (status === 'ASSIGNED') {
    buttons.push(createStatusButton(booking.token, 'IN_PROGRESS', 'Iniciar', 'warning'));
  }

  if (status === 'IN_PROGRESS') {
    buttons.push(createStatusButton(booking.token, 'COMPLETED', 'Concluir', 'success'));
  }

  if (status !== 'CANCELLED' && status !== 'COMPLETED') {
    buttons.push(createStatusButton(booking.token, 'CANCELLED', 'Cancelar', 'error'));
  }

  return buttons.join('');
}

/**
 * Cria um botão de histórico
 */
function createHistoryButton(token, history) {
  const historyCount = history && Array.isArray(history) ? history.length : 0;
  // Escapar o histórico para ser passado como atributo data
  const historyJson = escapeHtml(JSON.stringify(history || []));
  return `
        <button 
            class="btn btn-outline btn-action btn-action-small" 
            data-token="${escapeHtml(token)}"
            data-history='${historyJson}'
            onclick="showHistoryFromData('${escapeHtml(token)}', this)"
            title="${historyCount} transições de estado"
        >
            📋 Histórico
        </button>
    `;
}

/**
 * Cria um botão de mudança de status
 */
function createStatusButton(token, newStatus, label, type) {
  const buttonClass = type === 'error' ? 'btn-danger' : type === 'success' ? 'btn-primary' : 'btn-secondary';
  return `
        <button 
            class="btn ${buttonClass} btn-action btn-action-small" 
            data-token="${escapeHtml(token)}"
            data-status="${newStatus}"
            onclick="updateBookingStatus('${escapeHtml(token)}', '${newStatus}')"
        >
            ${label}
        </button>
    `;
}

/**
 * Atualiza o status de uma reserva
 * Função global para ser chamada por onclick
 */
window.updateBookingStatus = async function (token, newStatus) {
  if (!confirm(`Tem certeza que deseja atualizar o status para "${newStatus}"?`)) {
    return;
  }

  try {
    const url = `${STAFF_API_BASE}/${token}/status?status=${encodeURIComponent(newStatus)}`;

    const response = await fetch(url, {
      method: 'PATCH'
    });

    if (!response.ok) {
      const data = await response.json();
      throw new Error(data.message || 'Erro ao atualizar status');
    }

    showMessage(`Status atualizado com sucesso para "${newStatus}"!`, 'success');

    // Recarregar reservas após um pequeno delay
    setTimeout(() => {
      const municipalityFilter = document.getElementById('municipality-filter').value || 'all';
      loadBookings(municipalityFilter);
    }, 500);

  } catch (error) {
    console.error('Erro ao atualizar status:', error);
    showMessage(
      error.message || 'Erro ao atualizar status. Por favor, tente novamente.',
      'error'
    );
  }
}

/**
 * Atualiza o contador de reservas
 */
function updateBookingCount(count) {
  const totalCount = document.getElementById('total-count');
  totalCount.textContent = count;
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
  const messageContainer = document.getElementById('msg');

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

  // Remover mensagem após 5 segundos
  if (type === 'success') {
    setTimeout(() => {
      messageContainer.innerHTML = '';
    }, 5000);
  }
}

/**
 * Escapa HTML para prevenir XSS
 */
function escapeHtml(text) {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

/**
 * Inicializa o modal de histórico
 */
function initHistoryModal() {
  const modal = document.getElementById('history-modal');
  const overlay = document.getElementById('modal-overlay');
  const closeBtn = document.getElementById('modal-close');

  // Fechar ao clicar no overlay ou no botão de fechar
  if (overlay) {
    overlay.addEventListener('click', closeHistoryModal);
  }

  if (closeBtn) {
    closeBtn.addEventListener('click', closeHistoryModal);
  }

  // Fechar com ESC
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && !modal.classList.contains('hidden')) {
      closeHistoryModal();
    }
  });
}

/**
 * Mostra o modal de histórico (chamado pelo onclick)
 */
window.showHistoryFromData = function (token, buttonElement) {
  const historyJson = buttonElement.getAttribute('data-history');
  let history = [];
  try {
    history = JSON.parse(historyJson);
  } catch (e) {
    console.error('Erro ao parsear histórico:', e);
  }
  showHistory(token, history);
};

/**
 * Mostra o modal de histórico
 */
function showHistory(token, history) {
  const modal = document.getElementById('history-modal');
  const historyList = document.getElementById('history-list');

  if (!history || !Array.isArray(history) || history.length === 0) {
    historyList.innerHTML = `
      <div class="history-empty">
        <div class="history-empty-icon">📭</div>
        <p>Nenhum histórico disponível para este agendamento.</p>
      </div>
    `;
  } else {
    // Mapear labels de status
    const statusLabels = {
      'RECEIVED': 'Recebida',
      'ASSIGNED': 'Atribuída',
      'IN_PROGRESS': 'Em Progresso',
      'COMPLETED': 'Concluída',
      'CANCELLED': 'Cancelada'
    };

    historyList.innerHTML = history.map(item => {
      // Parsear formato "timestamp - STATUS"
      const parts = item.split(' - ');
      let timestamp = parts[0] || '';
      let status = parts[1] || '';

      // Formatar timestamp se possível
      try {
        const date = new Date(timestamp);
        if (!isNaN(date.getTime())) {
          timestamp = date.toLocaleString('pt-PT', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
          });
        }
      } catch (e) {
        // Manter timestamp original se não conseguir formatar
      }

      // Traduzir status
      const statusLabel = statusLabels[status] || status;

      return `
        <div class="history-item">
          <div class="history-timestamp">${escapeHtml(timestamp)}</div>
          <div class="history-status">${escapeHtml(statusLabel)}</div>
        </div>
      `;
    }).join('');
  }

  modal.classList.remove('hidden');
}

/**
 * Fecha o modal de histórico
 */
function closeHistoryModal() {
  const modal = document.getElementById('history-modal');
  modal.classList.add('hidden');
}
