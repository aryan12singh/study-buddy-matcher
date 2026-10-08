package com.studybuddy.profile;

import com.studybuddy.common.AccountAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProfileViewService {
    private final AccountAccess access;
    private final ProfileViewAssembler assembler;

    public ProfileViewService(AccountAccess access, ProfileViewAssembler assembler) {
        this.access = access;
        this.assembler = assembler;
    }

    public ProfileDto view(Long subjectId, Long viewerId) {
        access.requireStudent(viewerId);
        return assembler.assemble(access.eligibleStudent(subjectId), viewerId);
    }
}
