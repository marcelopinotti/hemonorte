package com.hemonorte.controller

import com.hemonorte.dto.*
import com.hemonorte.security.doadorAutenticado
import com.hemonorte.service.AgendamentoService
import com.hemonorte.service.DoadorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/doador")
class DoadorController(
    private val doadorService: DoadorService,
    private val agendamentoService: AgendamentoService? = null,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun cadastrar(@Valid @RequestBody request: CadastroDoadorRequest): DoadorResponse =
        doadorService.cadastrar(
            nome = request.nome,
            email = request.email,
            cpf = request.cpf,
            senha = request.senha,
            tipagem = request.tipagem,
            dataNascimento = request.dataNascimento,
            genero = request.genero.single(),
        ).toResponse()

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun listar(): List<DoadorResponse> = doadorService.listar().map { it.toResponse() }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('TIPO_DOADOR')")
    fun buscarMe(authentication: Authentication): DoadorResponse =
        doadorService.buscar(authentication.doadorAutenticado().id).toResponse()

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('TIPO_DOADOR')")
    fun atualizarMe(
        authentication: Authentication,
        @Valid @RequestBody request: AtualizacaoDoadorRequest,
    ): DoadorResponse = atualizar(authentication.doadorAutenticado().id, request)

    @PutMapping("/me/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TIPO_DOADOR')")
    fun alterarMinhaSenha(authentication: Authentication, @Valid @RequestBody request: AlteracaoSenhaRequest) {
        doadorService.alterarSenha(authentication.doadorAutenticado().id, request.senha)
    }

    @GetMapping("/me/agendamentos")
    @PreAuthorize("hasAuthority('TIPO_DOADOR')")
    fun meusAgendamentos(authentication: Authentication): List<AgendamentoResponse> =
        requireNotNull(agendamentoService).listarPorDoadorResponses(authentication.doadorAutenticado().id)

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRIAGEM', 'COLETA', 'ADMIN')")
    fun buscar(@PathVariable id: UUID): DoadorResponse = doadorService.buscar(id).toResponse()

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun atualizar(
        @PathVariable id: UUID,
        @Valid @RequestBody request: AtualizacaoDoadorRequest,
    ): DoadorResponse = doadorService.atualizar(
        id = id,
        nome = request.nome,
        email = request.email,
        tipagem = request.tipagem,
        dataNascimento = request.dataNascimento,
        genero = request.genero.single(),
    ).toResponse()

    @PutMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    fun alterarSenha(@PathVariable id: UUID, @Valid @RequestBody request: AlteracaoSenhaRequest) {
        doadorService.alterarSenha(id, request.senha)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    fun excluir(@PathVariable id: UUID) {
        doadorService.excluir(id)
    }
}
