Feature: Gestão de Agendamentos - Painel Staff
  Como staff do sistema ZeroMonos
  Eu quero listar, filtrar e atualizar o status dos agendamentos
  Para gerir eficientemente os agendamentos de recolha de resíduos

  Background:
    Dado que o sistema está disponível

  Scenario: Listar todos os agendamentos
    Dado que existem agendamentos no sistema
    Quando faço uma requisição GET para "/api/staff/bookings"
    Então o status da resposta deve ser 200
    E a resposta deve conter uma lista de agendamentos
    E a lista deve ter pelo menos 1 agendamento

  Scenario: Filtrar agendamentos por município
    Dado que existe um município chamado "Lisboa"
    E que existem agendamentos para "Lisboa"
    Quando faço uma requisição GET para "/api/staff/bookings" com parâmetro "municipality=Lisboa"
    Então o status da resposta deve ser 200
    E a resposta deve conter uma lista de agendamentos
    E todos os agendamentos devem ser do município "Lisboa"

  Scenario: Filtrar agendamentos por município inexistente
    Quando faço uma requisição GET para "/api/staff/bookings" com parâmetro "municipality=MunicipioInexistente"
    Então o status da resposta deve ser 404
    E a mensagem de erro deve conter "Município não encontrado"

  Scenario: Atualizar status de agendamento para ASSIGNED
    Dado que existe um agendamento com token "token-staff-teste"
    Quando faço uma requisição PATCH para "/api/staff/bookings/token-staff-teste/status" com status "ASSIGNED"
    Então o status da resposta deve ser 200
    E o status do agendamento na resposta deve ser "ASSIGNED"

  Scenario: Atualizar status de agendamento para IN_PROGRESS
    Dado que existe um agendamento com token "token-staff-teste-2"
    E que o status do agendamento foi atualizado para "ASSIGNED"
    Quando faço uma requisição PATCH para "/api/staff/bookings/token-staff-teste-2/status" com status "IN_PROGRESS"
    Então o status da resposta deve ser 200
    E o status do agendamento na resposta deve ser "IN_PROGRESS"

  Scenario: Atualizar status de agendamento para COMPLETED
    Dado que existe um agendamento com token "token-staff-teste-3"
    E que o status do agendamento foi atualizado para "IN_PROGRESS"
    Quando faço uma requisição PATCH para "/api/staff/bookings/token-staff-teste-3/status" com status "COMPLETED"
    Então o status da resposta deve ser 200
    E o status do agendamento na resposta deve ser "COMPLETED"

  Scenario: Atualizar status de agendamento inexistente
    Quando faço uma requisição PATCH para "/api/staff/bookings/token-inexistente-999/status" com status "ASSIGNED"
    Então o status da resposta deve ser 404
    E a mensagem de erro deve conter "não encontrado"

  Scenario: Fluxo completo de agendamento
    Dado que existe um município chamado "Aveiro"
    Quando crio um agendamento com os seguintes dados:
      | município         | Aveiro                    |
      | descrição         | Mesa de jantar            |
      | data solicitada   | amanhã                    |
      | período           | EVENING                   |
    E guardo o token do agendamento
    E atualizo o status do agendamento para "ASSIGNED"
    E atualizo o status do agendamento para "IN_PROGRESS"
    E atualizo o status do agendamento para "COMPLETED"
    Então quando consulto o agendamento pelo token guardado
    E o status do agendamento deve ser "COMPLETED"
    E o histórico deve conter pelo menos 4 mudanças de estado

