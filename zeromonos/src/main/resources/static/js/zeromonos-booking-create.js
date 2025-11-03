class BookingForm {
    constructor() {
        this.base = '/api/bookings';
        this.form = document.getElementById('booking-form');
        this.municipalityInput = document.getElementById('municipality');
        this.suggestionsDropdown = document.getElementById('suggestions-dropdown');
        this.messageContainer = document.getElementById('form-msg');
        this.municipalities = [];
        this.selectedIndex = -1;

        // Verificar se elementos existem
        if (!this.form || !this.municipalityInput || !this.suggestionsDropdown || !this.messageContainer) {
            console.error('Elementos do formulário não encontrados');
            return;
        }

        this.initializeForm();
    }

    async initializeForm() {
        try {
            await this.loadMunicipalities();
            this.form.addEventListener('submit', (e) => this.handleSubmit(e));
            this.setupAutocomplete();
            console.log('Formulário inicializado com sucesso');
        } catch (error) {
            console.error('Erro ao inicializar formulário:', error);
            this.showError('Erro ao inicializar formulário. Por favor, recarregue a página.');
        }
    }

    async loadMunicipalities() {
        const url = `${this.base}/municipalities`;
        console.log('🔍 [DEBUG] Iniciando carregamento de municípios...');
        console.log('🔍 [DEBUG] URL:', url);
        console.log('🔍 [DEBUG] Base URL:', window.location.origin);
        console.log('🔍 [DEBUG] URL completa:', window.location.origin + url);

        try {
            console.log('📡 [DEBUG] Fazendo fetch para:', url);
            const response = await fetch(url);

            console.log('📥 [DEBUG] Resposta recebida:');
            console.log('  - Status:', response.status);
            console.log('  - Status Text:', response.statusText);
            console.log('  - OK:', response.ok);
            console.log('  - Headers:', Object.fromEntries(response.headers.entries()));

            if (!response.ok) {
                const errorText = await response.text().catch(() => 'Não foi possível ler o corpo da resposta');
                console.error('❌ [DEBUG] Erro na resposta:', errorText);
                throw new Error(`Erro ao carregar municípios: ${response.status} ${response.statusText}`);
            }

            console.log('📋 [DEBUG] Parseando JSON...');
            const data = await response.json();
            console.log('📋 [DEBUG] Dados recebidos:', data);
            console.log('📋 [DEBUG] Tipo dos dados:', typeof data);
            console.log('📋 [DEBUG] É array?', Array.isArray(data));

            if (!Array.isArray(data)) {
                console.error('❌ [DEBUG] Dados não são um array:', data);
                throw new Error('Resposta inválida do servidor: não é um array');
            }

            if (data.length === 0) {
                console.warn('⚠️ [DEBUG] Array vazio recebido do servidor');
            }

            this.municipalities = data;
            console.log(`✅ [DEBUG] Carregados ${this.municipalities.length} municípios com sucesso`);
            console.log('✅ [DEBUG] Primeiros 5 municípios:', this.municipalities.slice(0, 5));

            // Verificar se municípios estão corretos
            if (this.municipalities.length > 0) {
                console.log('✅ [DEBUG] Exemplo de município:', this.municipalities[0]);
                console.log('✅ [DEBUG] Tipo do primeiro município:', typeof this.municipalities[0]);
            }

        } catch (error) {
            console.error('❌ [DEBUG] Erro completo ao carregar municípios:');
            console.error('  - Nome do erro:', error.name);
            console.error('  - Mensagem:', error.message);
            console.error('  - Stack:', error.stack);

            if (error instanceof TypeError) {
                console.error('  - Tipo: Erro de rede ou CORS');
            } else if (error instanceof SyntaxError) {
                console.error('  - Tipo: Erro ao parsear JSON');
            }

            this.showError('Erro ao carregar municípios: ' + (error.message || 'Erro desconhecido'));

            // Tentar novamente após 2 segundos
            setTimeout(() => {
                console.log('🔄 [DEBUG] Tentando carregar municípios novamente...');
                this.loadMunicipalities();
            }, 2000);
        }
    }

    setupAutocomplete() {
        console.log('🔧 [SETUP] Configurando autocomplete...');
        console.log('🔧 [SETUP] Input elemento:', this.municipalityInput);
        console.log('🔧 [SETUP] Dropdown elemento:', this.suggestionsDropdown);

        // Mostra sugestões ao digitar
        this.municipalityInput.addEventListener('input', (e) => {
            // Ignorar eventos de autofill de extensões
            if (e.isTrusted === false) {
                console.log('⚠️ [INPUT] Evento não confiável ignorado (pode ser de extensão)');
                return;
            }

            const value = e.target.value.trim();
            console.log('⌨️ [INPUT] Valor digitado:', value, 'Tamanho:', value.length);
            this.selectedIndex = -1;

            if (value.length === 0) {
                console.log('ℹ️ [INPUT] Input vazio, escondendo sugestões');
                this.hideSuggestions();
                return;
            }

            console.log('🔍 [INPUT] Chamando showSuggestions...');
            this.showSuggestions(value);
        });

        // Prevenir interferência de extensões no focus
        this.municipalityInput.addEventListener('focus', (e) => {
            // Forçar foco e garantir que nosso autocomplete tem prioridade
            console.log('🔍 [FOCUS] Campo de município recebeu foco');
            // Garantir que o dropdown esteja visível se houver texto
            if (this.municipalityInput.value.trim().length > 0 && this.municipalities.length > 0) {
                this.showSuggestions(this.municipalityInput.value.trim());
            }
        }, true);

        // Prevenir interferência de extensões ao selecionar
        this.municipalityInput.addEventListener('change', (e) => {
            // Ignorar mudanças causadas por extensões
            if (!e.isTrusted) {
                console.log('⚠️ [CHANGE] Evento de mudança não confiável ignorado (pode ser de extensão)');
                return;
            }
            console.log('🔄 [CHANGE] Valor mudado para:', e.target.value);
        });

        console.log('✅ [SETUP] Autocomplete configurado com sucesso');

        // Navegação por teclado
        this.municipalityInput.addEventListener('keydown', (e) => {
            const items = this.suggestionsDropdown.querySelectorAll('.suggestion-item');

            if (e.key === 'ArrowDown') {
                e.preventDefault();
                this.selectedIndex = Math.min(this.selectedIndex + 1, items.length - 1);
                this.updateSelectedItem(items);
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                this.selectedIndex = Math.max(this.selectedIndex - 1, -1);
                this.updateSelectedItem(items);
            } else if (e.key === 'Enter' && this.selectedIndex >= 0 && items[this.selectedIndex]) {
                e.preventDefault();
                e.stopPropagation();
                items[this.selectedIndex].click();
            } else if (e.key === 'Escape') {
                e.preventDefault();
                this.hideSuggestions();
                this.municipalityInput.blur();
            }
        });

        // Fecha ao clicar fora (usando mousedown para evitar conflito com click nas sugestões)
        document.addEventListener('mousedown', (e) => {
            if (!this.municipalityInput.contains(e.target) && !this.suggestionsDropdown.contains(e.target)) {
                this.hideSuggestions();
            }
        });
    }

    showSuggestions(query) {
        console.log('🔍 [AUTOCOMPLETE] Mostrando sugestões para:', query);
        console.log('🔍 [AUTOCOMPLETE] Municípios disponíveis:', this.municipalities.length);
        console.log('🔍 [AUTOCOMPLETE] Dropdown existe?', !!this.suggestionsDropdown);
        console.log('🔍 [AUTOCOMPLETE] Input existe?', !!this.municipalityInput);

        if (!this.suggestionsDropdown || !this.municipalities || this.municipalities.length === 0) {
            console.warn('⚠️ [AUTOCOMPLETE] Municípios não carregados ainda');
            console.warn('  - Dropdown existe?', !!this.suggestionsDropdown);
            console.warn('  - Municípios existe?', !!this.municipalities);
            console.warn('  - Número de municípios:', this.municipalities ? this.municipalities.length : 0);
            return;
        }

        console.log('🔍 [AUTOCOMPLETE] Filtrando municípios...');
        const filtered = this.municipalities.filter(m => {
            const match = m && m.toLowerCase().includes(query.toLowerCase());
            return match;
        });

        console.log(`✅ [AUTOCOMPLETE] Encontrados ${filtered.length} municípios correspondentes`);
        console.log('✅ [AUTOCOMPLETE] Municípios filtrados:', filtered.slice(0, 5));

        if (filtered.length === 0) {
            console.log('ℹ️ [AUTOCOMPLETE] Nenhum município encontrado para:', query);
            this.suggestionsDropdown.innerHTML = '<div class="suggestion-item no-suggestions"><em>Nenhum município encontrado</em></div>';
            this.suggestionsDropdown.classList.add('show', 'active');
            this.municipalityInput.setAttribute('aria-expanded', 'true');
            return;
        }

        this.suggestionsDropdown.innerHTML = filtered
            .slice(0, 10) // Limita a 10 sugestões
            .map(municipality =>
                `<div class="suggestion-item" 
                data-value="${this.escapeHtml(municipality)}"
                role="option">
            ${this.highlightMatch(municipality, query)}
           </div>`
            )
            .join('');

        // Adiciona evento de clique
        this.suggestionsDropdown.querySelectorAll('.suggestion-item').forEach(item => {
            item.addEventListener('click', (e) => {
                e.stopPropagation();
                if (this.municipalityInput && item.dataset.value) {
                    this.municipalityInput.value = item.dataset.value;
                    this.hideSuggestions();
                    this.municipalityInput.focus();
                    // Disparar evento de input para validar
                    this.municipalityInput.dispatchEvent(new Event('input', { bubbles: true }));
                }
            });

            // Hover para destacar
            item.addEventListener('mouseenter', () => {
                this.selectedIndex = Array.from(this.suggestionsDropdown.querySelectorAll('.suggestion-item')).indexOf(item);
                this.updateSelectedItem(this.suggestionsDropdown.querySelectorAll('.suggestion-item'));
            });
        });

        this.suggestionsDropdown.classList.add('show', 'active');
        this.municipalityInput.setAttribute('aria-expanded', 'true');
    }

    updateSelectedItem(items) {
        if (!items || items.length === 0) return;

        items.forEach((item, index) => {
            if (index === this.selectedIndex) {
                item.classList.add('selected', 'highlighted');
                item.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
            } else {
                item.classList.remove('selected', 'highlighted');
            }
        });
    }

    hideSuggestions() {
        if (this.suggestionsDropdown) {
            this.suggestionsDropdown.classList.remove('show', 'active');
            this.selectedIndex = -1;
            if (this.municipalityInput) {
                this.municipalityInput.setAttribute('aria-expanded', 'false');
            }
        }
    }

    highlightMatch(text, query) {
        // Escapar caracteres especiais do regex
        const escapedQuery = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        const regex = new RegExp(`(${escapedQuery})`, 'gi');
        return this.escapeHtml(text).replace(regex, '<strong>$1</strong>');
    }

    async handleSubmit(event) {
        event.preventDefault();
        const formData = new FormData(this.form);
        const data = Object.fromEntries(formData);

        try {
            const response = await fetch(this.base, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });

            if (!response.ok) {
                // tenta extrair JSON { message: '...' } do servidor
                let errMsg = null;
                try {
                    const errJson = await response.json();
                    errMsg = errJson && errJson.message ? errJson.message : null;
                } catch (e) {
                    // ignore parse error
                }
                const text = errMsg || await response.text() || `Erro ${response.status}`;
                throw new Error(text);
            }

            const result = await response.json();
            this.showSuccess(result.token);
            this.form.reset();
        } catch (error) {
            this.showError(error.message);
        }
    }

    showSuccess(token) {
        this.messageContainer.innerHTML = `
        <div class="message success">
          <p>Agendamento criado com sucesso!</p>
          <p style="display: flex; align-items: center; gap: 0.5rem;">
            Token: <strong>${this.escapeHtml(token)}</strong>
            <button onclick="navigator.clipboard.writeText('${this.escapeHtml(token)}'); this.innerHTML='✓ Copiado!'; setTimeout(() => this.innerHTML='📋 Copiar', 2000);" 
                    style="background: var(--success, #10b981); color: white; border: none; padding: 0.25rem 0.5rem; border-radius: 0.25rem; cursor: pointer; font-size: 0.875rem; white-space: nowrap;"
                    title="Copiar token">
              📋 Copiar
            </button>
          </p>
          <p><a href="/lookup-booking.html?token=${encodeURIComponent(token)}">Ver detalhes</a></p>
        </div>
      `;
    }

    showError(message) {
        this.messageContainer.innerHTML = `
        <div class="message error">
          <p>${this.escapeHtml(message)}</p>
        </div>
      `;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Inicializar quando o DOM estiver pronto
document.addEventListener('DOMContentLoaded', () => {
    new BookingForm();
});