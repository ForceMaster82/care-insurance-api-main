package kr.caredoc.careinsurance.security.accesscontrol

class CombinedSubject(private val subjects: Collection<Subject>) : Subject {
    override fun get(attribute: SubjectAttribute) = subjects.flatMap { it[attribute] }.toSet()
}
