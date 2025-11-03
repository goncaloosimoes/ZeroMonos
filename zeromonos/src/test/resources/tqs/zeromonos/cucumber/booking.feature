Feature: Gestão de Agendamentos de Recolha de Resíduos Volumosos
  Como utilizador do sistema ZeroMonos
  Eu quero agendar, consultar e cancelar recolhas de resíduos volumosos
  Para gerir os meus agendamentos de forma eficiente

  Background:
    Dado que o sistema está disponível

  Scenario: Listar municípios disponíveis
    Quando faço uma requisição GET para "/api/bookings/municipalities"
    Então o status da resposta deve ser 200
    E a resposta deve conter uma lista de municípios
    E a lista deve conter pelo menos "Lisboa" e "Porto"

  Scenario: Criar agendamento válido
    Dado que existe um município chamado "Lisboa"
    E que a data de amanhã não é domingo
    Quando crio um agendamento com os seguintes dados:
      | município         | Lisboa                    |
      | descrição         | Sofá velho e colchão       |
      | data solicitada   | amanhã                    |
      | período           | AFTERNOON                 |
    Então o status da resposta deve ser 200
    E a resposta deve conter um token
    E o status do agendamento deve ser "RECEIVED"
    E o município deve ser "Lisboa"

  Scenario: Criar agendamento com município inexistente
    Quando tento criar um agendamento com os seguintes dados:
      | município         | MunicipioInexistente      |
      | descrição          | Teste                     |
      | data solicitada    | amanhã                    |
      | período            | MORNING                   |
    Então o status da resposta deve ser 400
    E a mensagem de erro deve conter "não encontrado"

  Scenario: Criar agendamento com data no passado
    Dado que existe um município chamado "Lisboa"
    Quando tento criar um agendamento com os seguintes dados:
      | município         | Lisboa                    |
      | descrição          | Teste                     |
      | data solicitada    | ontem                     |
      | período            | MORNING                   |
    Então o status da resposta deve ser 400
    E a mensagem de erro deve conter "passado"

  Scenario: Criar agendamento para domingo
    Dado que existe um município chamado "Lisboa"
    Quando tento criar um agendamento para domingo
    Então o status da resposta deve ser 400
    E a mensagem de erro deve conter "fim de semana"

  Scenario: Consultar agendamento existente
    Dado que existe um agendamento com token "token-teste"
    Quando faço uma requisição GET para "/api/bookings/token-teste"
    Então o status da resposta deve ser 200
    E o token na resposta deve ser "token-teste"
    E o status do agendamento deve ser "RECEIVED"

  Scenario: Consultar agendamento inexistente
    Quando faço uma requisição GET para "/api/bookings/token-inexistente-12345"
    Então o status da resposta deve ser 404
    E a mensagem de erro deve conter "não encontrado"

  Scenario: Cancelar agendamento válido
    Dado que existe um agendamento cancelável com token "token-cancel-teste"
    Quando faço uma requisição PUT para "/api/bookings/token-cancel-teste/cancel"
    Então o status da resposta deve ser 204
    E quando consulto o agendamento pelo token "token-cancel-teste"
    Então o status do agendamento deve ser "CANCELLED"

  Scenario: Cancelar agendamento já cancelado
    Dado que existe um agendamento cancelado com token "token-ja-cancelado"
    Quando faço uma requisição PUT para "/api/bookings/token-ja-cancelado/cancel"
    Então o status da resposta deve ser 409
    E a mensagem de erro deve conter "não pode ser cancelado"

