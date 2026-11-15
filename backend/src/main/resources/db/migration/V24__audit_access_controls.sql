-- Protection complémentaire : les journaux sont consultables mais jamais modifiables.
REVOKE UPDATE, DELETE ON journal_audit FROM PUBLIC;
